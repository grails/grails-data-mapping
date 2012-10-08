/* Copyright (C) 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.gorm.neo4j

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher

import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.engine.Persister
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.transactions.Transaction

import org.grails.datastore.mapping.core.AbstractAttributeStoringSession
import javax.persistence.FlushModeType
import org.grails.datastore.mapping.query.Query
import java.util.concurrent.ConcurrentHashMap
import org.apache.commons.lang.NotImplementedException
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Simple
import org.codehaus.groovy.runtime.NullObject
import org.grails.datastore.mapping.model.types.OneToOne
import org.neo4j.graphdb.NotFoundException
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.Relationship
import org.springframework.util.ClassUtils
import java.util.concurrent.ConcurrentLinkedQueue
import org.grails.datastore.mapping.engine.event.PreInsertEvent
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.event.PreUpdateEvent
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.PreDeleteEvent
import org.grails.datastore.mapping.engine.event.PostInsertEvent
import org.grails.datastore.mapping.engine.event.PostUpdateEvent
import org.grails.datastore.mapping.engine.event.PostDeleteEvent
import org.grails.datastore.mapping.engine.event.PreLoadEvent
import org.grails.datastore.mapping.engine.event.PostLoadEvent
import org.grails.datastore.mapping.model.types.OneToMany
import org.grails.datastore.mapping.model.types.ToOne
import org.springframework.util.Assert
import org.grails.datastore.mapping.model.types.ManyToOne
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.collection.AbstractPersistentCollection
import org.grails.datastore.mapping.model.types.ManyToMany
import org.neo4j.graphdb.RelationshipType
import org.grails.datastore.mapping.model.types.Basic
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeEvent
import org.grails.datastore.mapping.query.api.QueryableCriteria
import org.springframework.beans.BeanWrapperImpl
import org.springframework.core.convert.ConverterNotFoundException

/**
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
class Neo4jSession extends AbstractAttributeStoringSession implements PropertyChangeListener {

    public static final TYPE_PROPERTY_NAME = "__type__"
    public static final String SUBREFERENCE_PROPERTY_NAME = "__subreference__"
    public static final String VERSION_PROPERTY = 'version'
    protected final Logger log = LoggerFactory.getLogger(getClass())

    static final ALLOWED_CLASSES_NEO4J_PROPERTIES = [
            null,
            NullObject,
            String,
            Integer,
            Long,
            Byte,
            Float,
            Short,
            Double,
            Boolean,
            String[],
            Integer[],
            Long[],
            Float[],
            Boolean.TYPE,
            Integer.TYPE,
            Long.TYPE,
            Byte.TYPE,
            Float.TYPE,
            Short.TYPE,
            Double.TYPE
    ]

    Transaction transaction  // defacto a Neo4jTransaction
    Datastore datastore  // defacto a Neo4jDatastore
    MappingContext mappingContext  // defacto a Neo4jMappingCOntext
    ApplicationEventPublisher applicationEventPublisher
    FlushModeType flushMode = FlushModeType.AUTO;

    protected Map<Serializable, Object> objectToKey = new ConcurrentHashMap<Serializable, Object>();
    protected inserts = new ConcurrentLinkedQueue()
    protected Map<Class, Persister> persisters = new ConcurrentHashMap<Class, Persister>();
    protected dirtyObjects = Collections.synchronizedSet(new HashSet())
    protected nonMonitorableObjects = Collections.synchronizedSet(new HashSet())

    @Override
    Transaction beginTransaction() {
        if (!transaction) {
            transaction = new Neo4jTransaction(nativeInterface)
        }
        transaction
    }

    @Override
    Serializable persist(Object o) {
        Assert.notNull o
        if (!o.id) {
            PersistentEntity pe = mappingContext.getPersistentEntity(o.class.name)
            def name = pe.javaClass.name

            Node node = nativeInterface.createNode()
            node.setProperty(TYPE_PROPERTY_NAME, name)
            node.setProperty(VERSION_PROPERTY, 0);

            Node subSubReferenceNode = findOrCreateSubSubReferenceNode(datastore.subReferenceNodes[name])
            Assert.notNull subSubReferenceNode

            subSubReferenceNode.createRelationshipTo(node, GrailsRelationshipTypes.INSTANCE)
            if (log.debugEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
                log.debug "created node $node.id with class $name"
            }
            o.id = node.id
            inserts << o

            EntityAccess entityAccess = new EntityAccess(pe, o)
            for (Association association in pe.associations) {
                def value = entityAccess.getProperty(association.name)
                switch (association) {
                    case ToOne:
                        if (value && !value.id) {
                            persist(value)
                        }

                        if ((value != null) && association.bidirectional) {
                            EntityAccess reverseEntityAccess = new EntityAccess(association.associatedEntity, value)
                            addObjectToReverseSide(reverseEntityAccess, association, o)
                        }
                        break
                    case ManyToMany:
                    case OneToMany:
                        if (value) {
                            value.findAll { !it.id }.each {
                                persist(it)
                                if (association.bidirectional) {
                                    EntityAccess reverseEntityAccess = new EntityAccess(association.associatedEntity, it)
                                    addObjectToReverseSide(reverseEntityAccess, association, o)
                                }
                            }
                        }
                        break
                    default:
                        throw new NotImplementedException()
                }
            }
        }
        objectToKey[o.id] = o
        monitorSettersForObject(o)
        o.id
    }

    /**
     * return a subSubReference node for the current operation.
     * Current strategy is to use current thread's is module 128
     * @param subSubReferenceMap
     * @return
     */
    Node findOrCreateSubSubReferenceNode(Node subReferenceNode) {
        int hashValue = Thread.currentThread().id % Neo4jDatastore.NUMBER_OF_SUBSUBREFERENCE_NODES
        def subSubReferenceNode = subReferenceNode.getRelationships(GrailsRelationshipTypes.SUBSUBREFERENCE, Direction.OUTGOING).find {
            it.getProperty("hash", null) == hashValue
        }?.endNode
        if (!subSubReferenceNode) {
            subSubReferenceNode = datastore.graphDatabaseService.createNode()
            Relationship rel = subReferenceNode.createRelationshipTo(subSubReferenceNode, GrailsRelationshipTypes.SUBSUBREFERENCE)
            rel.setProperty("hash", hashValue)
        }
        return subSubReferenceNode
        //subSubReferenceMap.get(subSubReferneceKey)
    }

    @Override
    void refresh(Object o) {
        throw new NotImplementedException()
    }

    @Override
    void attach(Object o) {
        Assert.notNull o.id
        objectToKey[o.id] = o
        monitorSettersForObject(o)
    }

    private boolean isProxy(object) {
        object.metaClass.getMetaMethod("isProxy", null) != null
    }

    @Override
    void flush() {
        // do not iterate directly since we might add some entities to objects collection during iteration
        def objects = [] as ConcurrentLinkedQueue
        objects.addAll(nonMonitorableObjects)
        objects.addAll(dirtyObjects)
        objects.addAll(inserts)

        if (log.infoEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
            log.info "pre flush counting: nonMonitorable: ${nonMonitorableObjects.size()}, dirty: ${dirtyObjects.size()}, inserts: ${inserts.size()}, total: ${objects.size()}"
        }

        def alreadyPersisted = [] as Set // required to prevent doubled (and cyclic) saves

        def persistedCounter = 0

        while (!objects.empty) {
            def obj = objects.poll()
            def id = obj.id
            if ((obj in alreadyPersisted) || isProxy(obj)) {
                continue
            }
            if (log.debugEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
                log.debug "flush obj ${obj.getClass().name} $id"
            }
            alreadyPersisted << obj
            PersistentEntity pe = mappingContext.getPersistentEntity(obj.getClass().name)

            def entityAccess = new EntityAccess(pe, obj)
            AbstractPersistenceEvent event = inserts.contains(obj) ?
                new PreInsertEvent(datastore, pe, entityAccess) : new PreUpdateEvent(datastore, pe, entityAccess)
            applicationEventPublisher.publishEvent(event)
            if (event.cancelled) {
                continue
            }

            boolean hasChanged = false

            Node thisNode = nativeInterface.getNodeById(obj.id)
            for (PersistentProperty prop in pe.persistentProperties) {
                def value = entityAccess.getProperty(prop.name)
                switch (prop) {
                    case Simple:
                        if (!ALLOWED_CLASSES_NEO4J_PROPERTIES.contains(prop.type)) {
                            try {
                                value = mappingContext.conversionService.convert(value, Long.TYPE)
                            } catch (ConverterNotFoundException e) {
                                value = mappingContext.conversionService.convert(value, String)
                            }

                        }
                        if (thisNode.getProperty(prop.name, null) != value) {
                            value == null ? thisNode.removeProperty(prop.name) : thisNode.setProperty(prop.name, value)
                            hasChanged = true
                        }
                        if (log.debugEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
                            log.debug "storing simple for $prop = $value ${thisNode.getProperty(prop.name, null)?.getClass()}"
                        }
                        break

                    case ToOne:
                        def toAdd = writeToOneProperty(prop, thisNode, value, obj)
                        if (toAdd) {
                            objects << toAdd
                        }
                        break

                    case ManyToMany:
                    case OneToMany:
                        objects.addAll(writeToManyProperty(value, prop, thisNode, obj))
                        break

                    case Basic:
                        if (prop.type instanceof Collection) {
                            objects.addAll(writeToManyProperty(value, prop, thisNode, obj))
                        }
                        else {
                            def toAdd = writeToOneProperty(prop, thisNode, value, obj)
                            if (toAdd) {
                                objects << toAdd
                            }
                        }
                        break

                    default:
                        throw new NotImplementedException("don't know how to store $prop ${prop.getClass().superclass}")

                }
            }

            if (hasChanged && (!inserts.contains(obj))) {
                def version = thisNode.getProperty(VERSION_PROPERTY) + 1
                thisNode.setProperty(VERSION_PROPERTY, version)
                entityAccess.setProperty(VERSION_PROPERTY, version)
            }

            //def entityAccess = new EntityAccess(pe, obj)
            event = inserts.contains(obj) ?
                new PostInsertEvent(datastore, pe, entityAccess) : new PostUpdateEvent(datastore, pe, entityAccess)
            applicationEventPublisher.publishEvent(event) // TODO: hotspot
            persistedCounter++
        }
        inserts.clear()
        dirtyObjects.clear()
        if (log.infoEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
            log.info "post flush counting: persisted: $persistedCounter"
        }
    }

    private def writeToManyProperty(value, Association association, Node thisNode, obj) {
        def returnValue = []
        boolean doPersist = true
        if (value == null) {
            doPersist = false
        } else {
            // prevent persisting an not initialized APS
            if (value instanceof AbstractPersistentCollection) {
                doPersist = value.initialized
            }
        }

        if (doPersist) {
            def nodesIds = value?.collect {

                if (!it.id) { // if referenced obj is not yet persisted, add it and append it to flush chain
                    persist(it)
                    returnValue << it
                }

                if (association.bidirectional) {
                    EntityAccess reverseEntityAccess = new EntityAccess(association.associatedEntity, it)
                    addObjectToReverseSide(reverseEntityAccess, association, obj)
                }
                it.node.id
            } ?: []

            def (relationshipType, direction) = Neo4jUtils.relationTypeAndDirection(association)

            def existingNodesIds = []
            
            for (Relationship rel in thisNode.getRelationships(relationshipType, direction).iterator()) {
                def target = rel.getOtherNode(thisNode).id
                if (target in nodesIds) {
                    existingNodesIds << target
                } else {
                    rel.delete()
                    if (log.debugEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
                        log.debug "delete relationship ${rel.startNode.id} -> ${rel.endNode.id} ($rel.type.name()}"
                    }
                }
            }

            (nodesIds - existingNodesIds).each {
                Node startNode = thisNode
                Node endNode = nativeInterface.getNodeById(it)
                if (direction == Direction.INCOMING) {
                    (startNode, endNode) = [endNode, startNode]
                }
                startNode.createRelationshipTo(endNode, relationshipType)
                if (log.debugEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
                    log.debug "created relationship ${startNode.id} -> ${endNode.id} ($relationshipType}"
                }
            }
        }
        returnValue
    }

    private def writeToOneProperty(Association association, Node thisNode, value, obj) {
        def returnValue = null
        if ((value != null) && (!value.id)) {
            persist(value)
            returnValue = value
        }

        def (relationshipType, direction) = Neo4jUtils.relationTypeAndDirection(association)
        Relationship rel = findRelationshipWithMatchingType(thisNode, relationshipType, direction, association.type)

        def endNodeId = rel?.getOtherNode(thisNode)?.id
        if (endNodeId && ((value == null) || (value.id != endNodeId))) {
            rel.delete()
            if (log.debugEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
                log.debug "delete relationship ${rel.startNode.id} -> ${rel.endNode.id} ($rel.type.name()}"
            }
        }

        if ((value != null) && (value.id != endNodeId)) {
            Node startNode = thisNode
            Node endNode = value?.node
            if (direction == Direction.INCOMING) {
                (startNode, endNode) = [endNode, startNode]
            }
            startNode.createRelationshipTo(endNode, relationshipType)
            if (log.debugEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
                log.debug "created relationship ${startNode.id} -> ${endNode.id} ($relationshipType}"
            }

            if (association.bidirectional) {
                def referencePropertyAccess = new EntityAccess(association.associatedEntity, value)
                switch (association) {
                    case OneToOne:
                        referencePropertyAccess.setProperty(association.referencedPropertyName, obj)
                        //value."${prop.referencedPropertyName}" = obj
                        break
                    case ManyToOne:
                        addObjectToReverseSide(referencePropertyAccess, association, obj)
                        break
                    default:
                        throw new NotImplementedException("setting inverse side of bidi, ${association.getClass().superclass}")
                }
            }
        }
        returnValue
    }

    @Override
    void clear() {
        objectToKey.values().each { unmonitorSettersForObject(it)}
        objectToKey.clear()
        dirtyObjects.clear()
        inserts.clear()
    }

    @Override
    void clear(Object o) {
        unmonitorSettersForObject(o)
        objectToKey.remove(o.id)
        inserts.remove(o)
        dirtyObjects.remove(o)
    }

    @Override
    boolean contains(Object o) {
        objectToKey.containsValue(o)
    }

    @Override
    void lock(Object o) {
        // hack recommended from neo4j guys: remove an non-existing property from a node to lock it
        Node node = o.node
        node.removeProperty('__notexisting__')
    }

    @Override
    void unlock(Object o) {
        // Unlocking happens upon transaction termination
    }

    @Override
    List<Serializable> persist(Iterable objects) {
        objects.collect { persist(it) }
    }

    /**
     * @param node
     * @return {@link PersistentEntity} matching the node's {@link org.grails.datastore.gorm.neo4j.Neo4jSession#TYPE_PROPERTY_NAME} or null
     */
    private PersistentEntity getTypeForNode(Node node) {
        String nodeType = node.getProperty(TYPE_PROPERTY_NAME, null)
        nodeType == null ? null : mappingContext.getPersistentEntity(nodeType)
    }

    private Relationship findRelationshipWithMatchingType(Node node, RelationshipType relationshipType, Direction direction, Class type) {
        node.getRelationships(relationshipType, direction).iterator().find {
            def otherNode = it.getOtherNode(node)
            PersistentEntity pe = getTypeForNode(otherNode)
            type.isAssignableFrom(pe.javaClass)
        }
    }

    @Override
    def <T> T retrieve(Class<T> type, Serializable key) {
        if (log.debugEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
            log.debug "retrieving $type for id $key"
        }
        def id = key as long
        if (id == null) {
            return null
        }
        def result = objectToKey[id]
        if ((result == null) || isProxy(result)) {
            try {
                Node node = nativeInterface.getNodeById(id)
                PersistentEntity pe = getTypeForNode(node)
                if ((pe == null) || (type && !type.isAssignableFrom(pe.javaClass))) {
                    return null
                }
                result = pe.javaClass.newInstance()
                result.id = id

                def entityAccess = new EntityAccess(pe, result)
                applicationEventPublisher.publishEvent(new PreLoadEvent(datastore, pe, entityAccess))

                for (PersistentProperty prop in pe.persistentProperties) {
                    switch (prop) {
                        case Simple:
                            def value = node.getProperty(prop.name, null)
                            value = mappingContext.conversionService.convert(value, prop.type)
                            entityAccess.setProperty(prop.name, value)
                            if (log.debugEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
                                log.debug "reading simple for $prop $value"
                            }
                            break

                        case ToOne:
                            readToOneProperty(prop, node, entityAccess)
                            break

                        case OneToMany:
                        case ManyToMany:
                            readToManyProperty(prop, node, entityAccess)
                            break

                        case Basic:
                            if (prop.type instanceof Collection) {
                                readToManyProperty(prop, node, entityAccess)
                            }
                            else {
                                readToOneProperty(prop, node, entityAccess)
                            }

                            default:
                            throw new NotImplementedException("don't know how to read $prop ${prop.getClass().superclass}")
                    }
                }
                applicationEventPublisher.publishEvent(new PostLoadEvent(datastore, pe, entityAccess))
                objectToKey[id] = result
                monitorSettersForObject(result)
            } catch (NotFoundException e) {
                log.warn "no node for $id found: $e.message"
                return null
            }
        }
        if (log.debugEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
            log.debug "returning for $key ${System.identityHashCode(result)}"
        }
        result
    }


    static memoizePropertyChangeListener = Collections.synchronizedMap([:].withDefault() { false }) // it.respondsTo("addPropertyChangeListener")})
    //def instanceSupportsPropertyChangeListener = { Class clazz -> clazz.respondsTo("addPropertyChangeListener")}.memoize()

    private void monitorSettersForObject(object) {
        if (memoizePropertyChangeListener[object.class] == true) {
            object.addPropertyChangeListener(this)
        } else {
            nonMonitorableObjects << object
        }
    }

    private void unmonitorSettersForObject(object) {
        if (memoizePropertyChangeListener[object.class] == true) {
            object.removePropertyChangeListener(this)
        } else {
            nonMonitorableObjects.remove(object)
        }
    }

    private def readToManyProperty(Association association, Node node, EntityAccess entityAccess) {
        def (relationshipType, direction) = Neo4jUtils.relationTypeAndDirection(association)
        def keys = node.getRelationships(relationshipType, direction).iterator().collect { it.getOtherNode(node).id }

        def collection = List.class.isAssignableFrom(association.type) ?
            new ObservableListWrapper(entityAccess.entity, keys, association.associatedEntity.javaClass, this) :
            new ObservableSetWrapper(entityAccess.entity, keys, association.associatedEntity.javaClass, this)
        entityAccess.setPropertyNoConversion(association.name, collection)
    }

    private def readToOneProperty(Association association, Node node, EntityAccess entityAccess) {
        def (relationshipType, direction) = Neo4jUtils.relationTypeAndDirection(association)
        def rel = findRelationshipWithMatchingType(node, relationshipType, direction, association.type)
        if (rel) {
            Node end = rel.getOtherNode(node)
            def value = objectToKey[end.id]
            if (value == null) {
                value = proxy(ClassUtils.forName(end.getProperty(TYPE_PROPERTY_NAME, null)), end.id)
            }
            entityAccess.setProperty(association.name, value)
        }
    }

    @Override
    def <T> T proxy(Class<T> type, Serializable key) {
        if (log.debugEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
            log.debug "creating proxy for $type, id $key"
        }
        mappingContext.proxyFactory.createProxy(this, type, key)
    }

    @Override
    def <T> T lock(Class<T> type, Serializable key) {
        throw new NotImplementedException()
    }

    @Override
    void delete(Iterable objects) {
        for (instance in objects) {
            delete(instance)
        }
    }

    @Override
    void delete(Object obj) {
        PersistentEntity pe = mappingContext.getPersistentEntity(obj.getClass().name)

        def entityAccess = new EntityAccess(pe, obj)
        def event = new PreDeleteEvent(datastore, pe, entityAccess)
        applicationEventPublisher.publishEvent(event)
        if (event.cancelled) {
            return
        }

        Node node = obj.node
        for (Relationship rel in node.getRelationships().iterator()) {
            rel.delete()
        }
        node.delete()
        inserts.remove(obj)
        objectToKey.remove(obj.id)
        dirtyObjects.remove(obj)
        unmonitorSettersForObject(obj)

        event = new PostDeleteEvent(datastore, pe, entityAccess)
        applicationEventPublisher.publishEvent(event)

    }

    @Override
    List retrieveAll(Class type, Iterable keys) {
        keys.collect { retrieve(type, it)}
    }

    @Override
    List retrieveAll(Class type, Serializable... keys) {
        retrieveAll(type, keys)
    }

    @Override
    Query createQuery(Class type) {
        new Neo4jQuery(this, mappingContext.getPersistentEntity(type.name))
    }

    @Override
    GraphDatabaseService getNativeInterface() {
        datastore.graphDatabaseService
    }

    @Override
    Persister getPersister(Object o) {
        // copied from AbstractSession
        if (o == null) return null;
        Class cls = o instanceof Class ? o : (o instanceof PersistentEntity ? o.javaClass : o.getClass())
        Persister p = persisters.get(cls);
        if (p == null) {
            p = createPersister(cls, getMappingContext());
            if (p) {
                persisters[cls] = p
            }
        }
        p
    }

    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.name)
        entity ? new DummyEntityPersister(mappingContext, entity) : null
    }

    @Override
    boolean isDirty(Object instance) {
        throw new NotImplementedException()
    }

    private def addObjectToReverseSide(EntityAccess reverseEntityAccess, Association association, objectToSet) {
        def propertyValue = reverseEntityAccess.getProperty(association.referencedPropertyName)
        switch (association.inverseSide) {
            case OneToMany:
            case ManyToMany:
                if (!propertyValue) {
                    propertyValue = Set.class.isAssignableFrom(association.inverseSide.type) ? [] as Set : []
                    reverseEntityAccess.setPropertyNoConversion(association.referencedPropertyName, propertyValue)
                }
                if (!(objectToSet in propertyValue)) {
                    propertyValue << objectToSet
                }
                break
            case ToOne:
                reverseEntityAccess.setProperty(association.referencedPropertyName, objectToSet)
                break
            default:
                throw new NotImplementedException()
        }
        if (log.debugEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
            log.debug "addObjectToReverseSide: property value $propertyValue"
        }
    }


    def createInstanceForNode(nodeOrId) {
        def id = nodeOrId instanceof Node ? nodeOrId.id : nodeOrId as long
        retrieve(null, id)
    }

    @Override
    int deleteAll(QueryableCriteria criteria) {
        // TODO: suboptimal.. improve batch deletes
        int total = 0
        for (o in criteria.list()) {
            delete(o)
            total++
        }
        return total
    }

    @Override
    int updateAll(QueryableCriteria criteria, Map<String, Object> properties) {
        // TODO: suboptimal.. improve batch updates
        final results = criteria.list()
        int total = 0
        for (o in results) {
            total++
            def bean = new BeanWrapperImpl(o)
            bean.setPropertyValues(properties)
            persist(o)
        }
        return total
    }

    @Override
    void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        if (propertyChangeEvent.propertyName!=VERSION_PROPERTY) {
            dirtyObjects << propertyChangeEvent.source
        }
    }

}
