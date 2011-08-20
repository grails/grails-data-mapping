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
import org.grails.datastore.mapping.collection.PersistentList
import org.grails.datastore.mapping.model.types.ToOne
import org.springframework.util.Assert
import org.grails.datastore.mapping.model.types.ManyToOne
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.collection.PersistentSet
import org.grails.datastore.mapping.collection.AbstractPersistentCollection
import org.grails.datastore.mapping.model.types.ManyToMany
import org.neo4j.graphdb.RelationshipType

/**
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
class Neo4jSession extends AbstractAttributeStoringSession {

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
    protected Map<Class, Persister> persisters = new ConcurrentHashMap<Class,Persister>();

    @Override
    Transaction beginTransaction() {
        transaction = new Neo4jTransaction(nativeInterface)
        transaction
    }

    @Override
    Serializable persist(Object o) {
        Assert.notNull o
        if (!o.id) {
            PersistentEntity pe = mappingContext.getPersistentEntity(o.class.name)
            def name = pe.javaClass.name

            Node node = nativeInterface.createNode()
            node.setProperty(Neo4jEntityPersister.TYPE_PROPERTY_NAME, name)
            Node subreferenceNode = datastore.subReferenceNodes[name]
            Assert.notNull subreferenceNode
            subreferenceNode.createRelationshipTo(node, GrailsRelationshipTypes.INSTANCE)
            log.debug "created node $node.id with class $name"
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

                        if ((value!=null) && association.bidirectional) {
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
        o.id
    }

    @Override
    void refresh(Object o) {
        throw new NotImplementedException()
    }

    @Override
    void attach(Object o) {
        Assert.notNull o.id
        objectToKey[o.id] = o
    }

    private boolean isProxy(object) {
        object.metaClass.methods.any { it.name=='isProxy'}
    }

    @Override
    void flush() {

        // do not iterate directly since we might add some entities to objects collection during iteration
        def objects = [] as ConcurrentLinkedQueue
        objects.addAll(objectToKey.values())

        def alreadyPersisted = [] as Set // required to prevent doubled (and cyclic) saves

        while (!objects.empty) {
            def obj = objects.poll()
            def id = obj.id
            if ((obj in alreadyPersisted) || isProxy(obj)) {
                continue
            }
            log.debug "flush obj $obj $id"
            alreadyPersisted << obj
            PersistentEntity pe = mappingContext.getPersistentEntity(obj.getClass().name)

            def entityAccess = new EntityAccess(pe, obj)
            AbstractPersistenceEvent event = inserts.contains(obj) ?
                new PreInsertEvent(datastore, pe, entityAccess) : new PreUpdateEvent(datastore, pe, entityAccess)
            applicationEventPublisher.publishEvent(event)
            if (event.cancelled) {
                continue
            }

            for (PersistentProperty prop in pe.persistentProperties) {
                def value = entityAccess.getProperty(prop.name)
                Node thisNode = obj.node
                switch (prop) {
                    case Simple:
                        if ((prop.name=='version') && (!inserts.contains(obj))) {
                            value = value==null ? 0 : value+1
                            entityAccess.setProperty(prop.name, value)
                        }
                        if (!ALLOWED_CLASSES_NEO4J_PROPERTIES.contains(prop.type)) {
                            value = mappingContext.conversionService.convert(value, String)
                        }
                        if (thisNode.getProperty(prop.name, null)!=value) {
                            value==null ? thisNode.removeProperty(prop.name) : thisNode.setProperty(prop.name, value)
                        }
                        log.debug "storing simple for $prop = $value ${thisNode.getProperty(prop.name, null)?.getClass()}"
                        break

                    case ToOne:
                        ToOne toOne = prop
                        if (value!=null) {
                            persist(value)
                            objects << value
                        }

                        def (relationshipType, direction) = Neo4jUtils.relationTypeAndDirection(prop)
                        Relationship rel = findRelationshipWithMatchingType(thisNode, relationshipType, direction, prop.type)

                        def endNodeId = rel?.getOtherNode(thisNode)?.id
                        if (endNodeId && ((value==null) || ( value.id!=endNodeId ))) {
                            rel.delete()
                            log.debug "delete relationship ${rel.startNode.id} -> ${rel.endNode.id} ($rel.type.name()}"
                        }

                        if ((value!=null) && (value.id!=endNodeId)) {
                            Node startNode = thisNode
                            Node endNode = value?.node
                            if (direction==Direction.INCOMING) {
                                (startNode, endNode) = [endNode, startNode]
                            }
                            startNode.createRelationshipTo(endNode, relationshipType)
                            log.debug "created relationship ${startNode.id} -> ${endNode.id} ($relationshipType}"

                            if (prop.bidirectional) {
                                def referencePropertyAccess = new EntityAccess(toOne.associatedEntity, value)
                                switch (prop) {
                                    case OneToOne:
                                        referencePropertyAccess.setProperty(toOne.referencedPropertyName, obj)
                                        //value."${prop.referencedPropertyName}" = obj
                                        break
                                    case ManyToOne:
                                        addObjectToReverseSide(referencePropertyAccess, prop, obj)
                                        break
                                    default:
                                        throw new NotImplementedException("setting inverse side of bidi, ${prop.getClass().superclass}")

                                }
                            }
                        }
                        break

                    case ManyToMany:
                    case OneToMany:
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
                                persist(it)
                                objects << it
                                it.node.id
                            } ?: []

                            def (relationshipType, direction) = Neo4jUtils.relationTypeAndDirection(prop)

                            def existingNodesIds = []
                            thisNode.getRelationships(relationshipType,direction).each {
                                def target = it.getOtherNode(thisNode).id
                                if (target in nodesIds) {
                                    existingNodesIds << target
                                } else {
                                    it.delete()
                                    log.debug "delete relationship ${it.startNode.id} -> ${it.endNode.id} ($it.type.name()}"
                                }
                            }

                            (nodesIds - existingNodesIds).each {
                                Node startNode = thisNode
                                Node endNode = nativeInterface.getNodeById(it)
                                if (direction==Direction.INCOMING) {
                                    (startNode, endNode) = [endNode, startNode]
                                }
                                startNode.createRelationshipTo(endNode, relationshipType)
                                log.debug "created relationship ${startNode.id} -> ${endNode.id} ($relationshipType}"
                            }
                        }
                        break


                    default:
                        throw new NotImplementedException("don't know how to store $prop ${prop.getClass().superclass}")

                }
            }

            //def entityAccess = new EntityAccess(pe, obj)
            event = inserts.contains(obj) ?
                new PostInsertEvent(datastore, pe, entityAccess) : new PostUpdateEvent(datastore, pe, entityAccess)
            applicationEventPublisher.publishEvent(event)
        }
        inserts.clear()
    }

    @Override
    void clear() {
        objectToKey.clear()
        inserts.clear()
    }

    @Override
    void clear(Object o) {
        objectToKey.remove(o.id)
        inserts.remove(o)
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
     * @return {@link PersistentEntity} matching the node's {@link Neo4jEntityPersister#TYPE_PROPERTY_NAME} or null
     */
    private PersistentEntity getTypeForNode(Node node) {
        String nodeType = node.getProperty(Neo4jEntityPersister.TYPE_PROPERTY_NAME, null)
        nodeType == null ? null : mappingContext.getPersistentEntity(nodeType)
    }

    private Relationship findRelationshipWithMatchingType(Node node, RelationshipType relationshipType, Direction direction, Class type) {
        node.getRelationships(relationshipType, direction).find {
            def otherNode = it.getOtherNode(node)
            PersistentEntity pe = getTypeForNode(otherNode)
            type.isAssignableFrom(pe.javaClass)
        }
    }

    @Override
    def <T> T retrieve(Class<T> type, Serializable key) {
        log.debug "retrieving $type for id $key"
        def id = key as long
        def result = objectToKey[id]
        if ((result==null) || isProxy(result) ) {
            try {
                Node node = nativeInterface.getNodeById(id)
                PersistentEntity pe = getTypeForNode(node)
                if ((pe==null) || (type && !type.isAssignableFrom(pe.javaClass))) {
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
                            log.debug "reading simple for $prop $value"
                            break

                        case ToOne:
                            def (relationshipType, direction) = Neo4jUtils.relationTypeAndDirection(prop)
                            def rel = findRelationshipWithMatchingType(node, relationshipType, direction, prop.type)
                            if (rel) {
                                Node end = rel.getOtherNode(node)
                                def value = objectToKey[end.id]
                                if (value == null) {
                                    value = proxy(ClassUtils.forName(end.getProperty(Neo4jEntityPersister.TYPE_PROPERTY_NAME, null)), end.id)
                                }
                                entityAccess.setProperty(prop.name, value)
                            }
                            break

                        case OneToMany:
                        case ManyToMany:
                            def (relationshipType, direction) = Neo4jUtils.relationTypeAndDirection(prop)
                            def keys = node.getRelationships(relationshipType, direction).collect { it.getOtherNode(node).id }
                            def collection = List.class.isAssignableFrom(prop.type) ?
                                new PersistentList(keys, prop.associatedEntity.javaClass, this) :
                                new PersistentSet(keys, prop.associatedEntity.javaClass, this)
                            entityAccess.setPropertyNoConversion(prop.name, collection)
                            break

                        default:
                            throw new NotImplementedException("don't know how to read $prop ${prop.getClass().superclass}")
                    }
                }
                applicationEventPublisher.publishEvent(new PostLoadEvent(datastore, pe, entityAccess))
                objectToKey[id] = result
            } catch (NotFoundException e) {
                log.warn "no node for $id found: $e.message"
                return null
            }
        }
        log.debug "returning for $key ${System.identityHashCode(result)}"
        result
    }

    @Override
    def <T> T proxy(Class<T> type, Serializable key) {
        log.debug "creating proxy for $type, id $key"
        mappingContext.proxyFactory.createProxy(this, type, key)
    }

    @Override
    def <T> T lock(Class<T> type, Serializable key) {
        throw new NotImplementedException()
    }

    @Override
    void delete(Iterable objects) {
        throw new NotImplementedException()
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
        node.getRelationships().each {
            it.delete()
        }
        node.delete()
        inserts.remove(obj)
        objectToKey.remove(obj.id)

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
        //flush()
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
            //firstLevelCache.put(cls, new ConcurrentHashMap<Serializable, Object>());
            if (p) {
                persisters[cls] = p
            }
        }
        p
    }

    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.name)
        entity ? new Neo4jEntityPersister(mappingContext, entity, this, applicationEventPublisher) : null
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
        log.debug "addObjectToReverseSide: property value $propertyValue"
    }


    def createInstanceForNode(nodeOrId) {
        def id = nodeOrId instanceof Node ? nodeOrId.id : nodeOrId as long
        retrieve(null, id)
    }

}
