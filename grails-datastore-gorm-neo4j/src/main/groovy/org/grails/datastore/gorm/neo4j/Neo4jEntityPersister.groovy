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

import org.codehaus.groovy.runtime.NullObject
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.NotFoundException
import org.neo4j.graphdb.Relationship
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.convert.ConversionException
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.AssociationIndexer
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.NativeEntryEntityPersister
import org.grails.datastore.mapping.engine.PropertyValueIndexer
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ManyToMany
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.model.types.OneToMany
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.RelationshipType
import org.grails.datastore.mapping.model.types.ToOne

/**
 * Implementation of {@link org.grails.datastore.mapping.engine.EntityPersister} that uses Neo4j database
 * as backend.
 *
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
class Neo4jEntityPersister extends NativeEntryEntityPersister<Node, Long> {

    protected final Logger log = LoggerFactory.getLogger(getClass())

    public static final TYPE_PROPERTY_NAME = "__type__"
    public static final String SUBREFERENCE_PROPERTY_NAME = "__subreference__"

    GraphDatabaseService graphDatabaseService

    Neo4jEntityPersister(MappingContext mappingContext, PersistentEntity entity,
              Session session, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, session, publisher)
        graphDatabaseService = session.nativeInterface
    }

    @Override
    String getEntityFamily() {
        classMapping.entity.toString()
    }

    @Override
    protected void deleteEntry(String family, Long key, entry) {
        Node node = graphDatabaseService.getNodeById(key)
        node.getRelationships(Direction.BOTH).each {
            log.debug "deleting relationship $it.startNode -> $it.endNode : ${it.type.name()}"
            it.delete()
        }
        node.delete()
    }

    @Override
    protected Long generateIdentifier(PersistentEntity persistentEntity, Node entry) {
        entry.id
    }

    @Override
    PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        new Neo4jPropertyValueIndexer(persistentProperty: property, graphDatabaseService: graphDatabaseService)
    }

    @Override
    AssociationIndexer getAssociationIndexer(Node nativeEntry, Association association) {
        new Neo4jAssociationIndexer(nativeEntry: nativeEntry, association:association, graphDatabaseService: graphDatabaseService)
    }

    @Override
    protected Node createNewEntry(String family) {
        Node node = graphDatabaseService.createNode()
        node.setProperty(TYPE_PROPERTY_NAME, family)
        Node subreferenceNode = ((Neo4jDatastore)session.datastore).subReferenceNodes[family]
        assert subreferenceNode
        subreferenceNode.createRelationshipTo(node, GrailsRelationshipTypes.INSTANCE)
        log.debug("created node $node.id with class $family")
        node
    }

    @Override
    protected getEntryValue(Node nativeEntry, String key) {

        def result
        PersistentProperty persistentProperty = persistentEntity.getPropertyByName(key)

        if (persistentProperty instanceof Association) {

            if (log.debugEnabled) {
                nativeEntry.relationships.each {
                    log.debug("rels $nativeEntry.id  has relationship ${it.startNode.id} -> ${it.endNode.id}, type $it.type")
                }
            }

            def (relationshipType, direction) = Neo4jUtils.relationTypeAndDirection(persistentProperty)

            if ((persistentProperty instanceof ManyToMany) || (persistentProperty instanceof OneToMany)) {
                result = nativeEntry.getRelationships(relationshipType, direction).collect { it.getOtherNode(nativeEntry).id }
            }
            else if (persistentProperty instanceof ToOne) {

                def endNodes = nativeEntry.getRelationships(relationshipType, direction).collect {
                    it.getOtherNode(nativeEntry)
                }.findAll {
                    Neo4jUtils.doesNodeMatchType(it, persistentProperty.type)
                }
                assert endNodes.size() <= 1
                result = endNodes ? endNodes[0].id : null
                log.debug("getting property $key via relationship on $nativeEntry = $result")
            }
            else {
                throw new IllegalArgumentException("${persistentProperty.class.superclass} associations ($persistentProperty) not yet implemented")
            }

        } else {
            result = nativeEntry.getProperty(key, null)
            PersistentProperty pe = discriminatePersistentEntity(persistentEntity, nativeEntry).getPropertyByName(key)
            try {
                result = mappingContext.conversionService.convert(result, pe.type)
            } catch (ConversionException e) {
                log.error("prop $key: $e.message")
                throw e
            }
            log.debug("getting property $key on $nativeEntry = $result")
        }
        result
    }

    @Override
    protected void setEntryValue(Node nativeEntry, String key, value) {
        if (value == null || key == 'id') {
            return
        }

        PersistentProperty persistentProperty = persistentEntity.getPropertyByName(key)
        if (persistentProperty instanceof Association) {
            def (relationshipType, direction) = Neo4jUtils.relationTypeAndDirection(persistentProperty)

            if (persistentProperty instanceof ManyToMany) {
                // called when loading instances
                for (item in value) {
                    Node endNode = graphDatabaseService.getNodeById(item instanceof Long ? item : item.id)
                    for (Relationship rel in nativeEntry.getRelationships(relationshipType, Direction.OUTGOING)) {
                        rel.delete()
                    }
                    //nativeEntry.createRelationshipTo(endNode, relationshipType)
                    createRelationshipTo(nativeEntry, endNode, relationshipType)
                }
            }
            else if (persistentProperty instanceof ToOne) {
                log.info("setting $key via relationship to $value, assoc is $persistentProperty ${persistentProperty.getClass().superclass.simpleName}, bidi: $persistentProperty.bidirectional, owning: $persistentProperty.owningSide")

                Node startNode = nativeEntry
                Node target = graphDatabaseService.getNodeById(value instanceof Long ? value : value.id)
                Node endNode = target
                if (direction==Direction.INCOMING) {
                    (startNode,endNode) = [endNode,startNode]
                }

                Relationship rel
                if (persistentProperty.bidirectional) {
                    def existingRelationsships = nativeEntry.getRelationships(relationshipType, direction).findAll {
                        Neo4jUtils.doesNodeMatchType(it.getOtherNode(nativeEntry), persistentProperty.type)
                    }
                    assert existingRelationsships.size() <= 1
                    if (existingRelationsships) {
                        rel = existingRelationsships[0]
                    }
                }
                else {
                    rel = nativeEntry.getSingleRelationship(relationshipType, direction)
                }

                //def rels = nativeEntry.getRelationships(relationshipType, direction)
                if (rel) {
                    if (rel.getOtherNode(nativeEntry) == target) {
                        return // unchanged value
                    }
                    log.info "deleting relationship $rel.startNode -> $rel.endNode : ${rel.type.name()}"
                    rel.delete()
                }

                //rel = startNode.createRelationshipTo(endNode, relationshipType)
                rel = createRelationshipTo(startNode, endNode, relationshipType)
                log.info("createRelationship $rel.startNode.id (${rel.startNode.getProperty(TYPE_PROPERTY_NAME,null)})-> $rel.endNode.id (${rel.endNode.getProperty(TYPE_PROPERTY_NAME,null)}) type: ($rel.type)")
            }
            else {
                throw new IllegalArgumentException("handling ${persistentProperty.getClass().superclass.simpleName} not yet supported. key $key, value  $value, assoc is $persistentProperty")
            }
        }
        else {
            log.debug("setting property $key = $value ${value?.getClass()}")

            if (!isAllowedNeo4jType(value.getClass())) {
                value = mappingContext.conversionService.convert(value, String)
            }
            nativeEntry.setProperty(key, value)
        }
    }

    @Override
    protected Node retrieveEntry(PersistentEntity persistentEntity, String family, Serializable key) {
        try {
            Node node = graphDatabaseService.getNodeById(key)

            def type = node.getProperty(TYPE_PROPERTY_NAME, null)
            switch (type) {
                case null:
                    log.warn("node $key has no property 'type' - maybe a tranaction problem.")
                    return null
                    break
                case family:
                    return node
                    break
                default:
                    //mappingContext.persistentEntities.find
                    Class clazz = Thread.currentThread().contextClassLoader.loadClass(type)
                    persistentEntity.javaClass.isAssignableFrom(clazz) ? node : null
            }
        } catch (NotFoundException e) {
            log.warn("could not retrieve an Node for id $key")
            null
        }
    }

    /**
     * Neo4j does not need to do anything on storeEntry
     * @param persistentEntity The persistent entity
     * @param entityAccess The EntityAccess
     * @param storeId
     * @param nativeEntry The native form. Could be a a ColumnFamily, BigTable Entity etc.
     * @return
     */
    @Override
    protected Long storeEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Long storeId, Node nativeEntry) {
        assert storeId
        assert nativeEntry
        assert persistentEntity
        log.info "storeEntry $persistentEntity $storeId"
        storeId // TODO: not sure what to do here...
    }

    @Override
    protected void updateEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Long key, Node entry) {
        if (!entry.hasProperty("version")) {
            return
        }

        long newVersion = entry.getProperty("version") + 1
        entry.setProperty("version", newVersion)
        entityAccess.entity.version = newVersion
    }

    @Override
    protected void deleteEntries(String family, List<Long> keys) {
        log.error("delete $keys")
        throw new UnsupportedOperationException()
    }

    Query createQuery() {
        new Neo4jQuery(session, persistentEntity, this)
    }

    protected boolean isAllowedNeo4jType(Class clazz) {
        switch (clazz) {
            case null:
            case NullObject:
            case String:
            case Integer:
            case Long:
            case Byte:
            case Float:
            case Boolean:
            case String[]:
            case Integer[]:
            case Long[]:
            case Float[]:
                return true
                break
            default:
                return false
        }
    }

    @Override
    protected PersistentEntity discriminatePersistentEntity(PersistentEntity persistentEntity, Node nativeEntry) {
        String className = nativeEntry.getProperty(TYPE_PROPERTY_NAME, null)
        PersistentEntity targetEntity = mappingContext.getPersistentEntity(className)
        /*for (def entity = targetEntity; entity != persistentEntity || entity == null; entity = entity.parentEntity) {
            assert entity
        }*/
        targetEntity
    }

    @Override
    protected void setManyToMany(PersistentEntity persistentEntity, obj, Node nativeEntry,
            ManyToMany manyToMany, Collection associatedObjects, Map<Association, List<Serializable>> toManyKeys) {

        toManyKeys.put manyToMany, associatedObjects.collect { it.id ?: session.persist(it) }
    }

    @Override
    protected Collection getManyToManyKeys(PersistentEntity persistentEntity, object,
            Serializable nativeKey, Node nativeEntry, ManyToMany manyToMany) {
        def (relationshipType, direction) = Neo4jUtils.relationTypeAndDirection(manyToMany)
        nativeEntry.getRelationships(relationshipType, direction).collect { it.getOtherNode(nativeEntry).id }
    }

    private Relationship createRelationshipTo(Node start, Node end, RelationshipType type) {
        if (start.getRelationships(type, Direction.OUTGOING).find { it.endNode == end}) {
            log.error "duplicate relationship detected before write: $start.id (${start.getProperty(TYPE_PROPERTY_NAME,null)})-> $end.id (${end.getProperty(TYPE_PROPERTY_NAME,null)}) type: ($type)"
        }
        start.createRelationshipTo(end, type)
    }
}
