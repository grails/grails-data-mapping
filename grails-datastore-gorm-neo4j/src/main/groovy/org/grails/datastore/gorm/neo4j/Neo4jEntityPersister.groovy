package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.engine.NativeEntryEntityPersister
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.engine.PropertyValueIndexer
import org.springframework.datastore.mapping.model.PersistentProperty
import org.springframework.datastore.mapping.engine.AssociationIndexer
import org.springframework.datastore.mapping.model.types.Association
import org.springframework.datastore.mapping.engine.EntityAccess
import org.springframework.datastore.mapping.query.Query
import org.springframework.context.ApplicationEventPublisher
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.model.MappingContext
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.neo4j.graphdb.NotFoundException
import org.codehaus.groovy.runtime.NullObject
import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.graphdb.Direction
import org.springframework.core.convert.ConversionException
import org.apache.commons.lang.NotImplementedException

/**
 * Implementation of {@link org.springframework.datastore.mapping.engine.EntityPersister} that uses Neo4j database
 * as backend.
 *
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
class Neo4jEntityPersister extends NativeEntryEntityPersister {

    private static final Logger log = LoggerFactory.getLogger(Neo4jEntityPersister.class);
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
    protected void deleteEntry(String family, Object key) {
        def node = graphDatabaseService.getNodeById(key)
        node.getRelationships(Direction.BOTH).each {
            log.info "deleting relationship $it.startNode -> $it.endNode : ${it.type.name()}"
            it.delete()
        }
        node.delete()
    }

    @Override
    protected Object generateIdentifier(PersistentEntity persistentEntity, Object entry) {
        entry.id
    }

    @Override
    PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        [
                index: {k,v -> },
        ] as PropertyValueIndexer
    }

    @Override
    AssociationIndexer getAssociationIndexer(Object nativeEntry, Association association) {
        new Neo4jAssociationIndexer(nativeEntry: nativeEntry, association:association, graphDatabaseService: graphDatabaseService)
    }

    @Override
    protected Object createNewEntry(String family) {
        Node node = graphDatabaseService.createNode()
        node.setProperty(TYPE_PROPERTY_NAME, family)
        def subreferenceNode = session.datastore.subReferenceNodes[family]
        assert subreferenceNode
        subreferenceNode.createRelationshipTo(node, GrailsRelationshipTypes.INSTANCE)
	    log.info("created node $node.id with class $family")
        node
    }

    @Override
    protected Object getEntryValue(Object nativeEntry, String property) {
	    def result
	    if (persistentEntity.associations.find { it.name == property } ) {
		    def relname = DynamicRelationshipType.withName(property)

            if (log.infoEnabled) {
                nativeEntry.relationships.each {
                    log.info("rels $nativeEntry.id  has relationship ${it.startNode.id} -> ${it.endNode.id}, type $it.type")
                }
            }

		    def rel = nativeEntry.getSingleRelationship(relname, Direction.OUTGOING)
		    result = rel ? rel.getOtherNode(nativeEntry).id : null
		    log.info("getting property $property via relationship on $nativeEntry = $result")
	    } else {
		    result = nativeEntry.getProperty(property, null)
            def pe = discriminatePersistentEntity(persistentEntity, nativeEntry).getPropertyByName(property)
		    try {
		        result = mappingContext.conversionService.convert(result, pe.type)
            } catch (ConversionException e) {
                log.error("prop $property: $e.message")
                throw e
            }
		    log.debug("getting property $property on $nativeEntry = $result")
	    }
	    result
    }

	@Override
	protected void setEntryValue(Object nativeEntry, String key, Object value) {
		if ((value != null) && (key!='id')) {
			if (persistentEntity.associations.find { it.name == key } ) {
				log.info("setting $key via relationship to $value")

				def relname = DynamicRelationshipType.withName(key)
				def rel = nativeEntry.getSingleRelationship(relname, Direction.OUTGOING)
				if (rel) {
					if (rel.endNode.id == value) {
						return // unchanged value
					}
                    log.info "deleting relationship $rel.startNode -> $rel.endNode : ${rel.type.name()}"
					rel.delete()
				}

                def targetNodeId = value instanceof Long ? value : value.id
				def targetNode = graphDatabaseService.getNodeById(targetNodeId)
				rel = nativeEntry.createRelationshipTo(targetNode, relname)
                log.warn("createRelationship $rel.startNode.id -> $rel.endNode.id ($rel.type)")

			} else {
				log.debug("setting property $key = $value ${value?.class}")

				if (!isAllowedNeo4jType(value.class)) {
					value = mappingContext.conversionService.convert(value, String)
				}
				nativeEntry.setProperty(key, value)

                def persistentProperty = persistentEntity.getPropertyByName(key)
                if (persistentProperty) {
                    if (persistentProperty.mapping.mappedForm.index) {
                        log.info "$key is indexed!"
                        def index = graphDatabaseService.index().forNodes(persistentEntity.name)
                        index.remove(nativeEntry, key)
                        index.add(nativeEntry, key, value)
                    }
                }
			}
		}
	}

	@Override
    protected Object retrieveEntry(PersistentEntity persistentEntity, String family, Serializable key) {
        try {
            def node = graphDatabaseService.getNodeById(key)

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
    protected Object storeEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object storeId, Object nativeEntry) {
        assert storeId
        assert nativeEntry
        assert persistentEntity
        storeId // TODO: not sure what to do here...
    }

    @Override
    protected void updateEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object key, Object entry) {
        if (entry.hasProperty("version")) {
            def newVersion = entry.getProperty("version") + 1
            entry.setProperty("version", newVersion)
            entityAccess.entity.version = newVersion
        }
    }

    @Override
    protected void deleteEntries(String family, List keys) {
        log.error("delete $keys")
        throw new NotImplementedException()
    }

    Query createQuery() {
        new Neo4jQuery(session, persistentEntity, this)
    }

    private boolean isAllowedNeo4jType(Class clazz) {
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
                return true;
                break
            default:
                return false;
        }
    }

    @Override
    protected PersistentEntity discriminatePersistentEntity(PersistentEntity persistentEntity, Object nativeEntry) {
        def className = nativeEntry.getProperty(TYPE_PROPERTY_NAME,null)
        def targetEntity = mappingContext.getPersistentEntity(className)
        for (def entity = targetEntity; (entity != persistentEntity) || (entity==null); entity = entity.getParentEntity()) {
            assert entity
        }
        return targetEntity
    }

}
