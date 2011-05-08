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
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 25.04.11
 * Time: 18:09
 * To change this template use File | Settings | File Templates.
 */
class Neo4jEntityPersister extends NativeEntryEntityPersister {

    private static final Logger LOG = LoggerFactory.getLogger(Neo4jEntityPersister.class);
	public static final TYPE_PROPERTY_NAME = "__type__"
	public static final String SUBREFERENCE_PROPERTY_NAME = "__subreference__"

	GraphDatabaseService graphDatabaseService

    Neo4jEntityPersister(MappingContext mappingContext, PersistentEntity entity,
              Session session, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, session, publisher)
        graphDatabaseService = session.nativeInterface
        createSubReferenceNode(session, entity)
    }

    def createSubReferenceNode(session,entity) {
        def name = entityFamily
        if (!session.subReferenceNodes.containsKey(name)) {
            def subReferenceNode = graphDatabaseService.createNode()
            subReferenceNode.setProperty(SUBREFERENCE_PROPERTY_NAME, name)
            graphDatabaseService.referenceNode.createRelationshipTo(subReferenceNode, GrailsRelationshipTypes.SUBREFERENCE)
            session.subReferenceNodes[name] = subReferenceNode
        }
    }

    @Override
    String getEntityFamily() {
        classMapping.entity.toString()
    }

    @Override
    protected void deleteEntry(String family, Object key) {
        throw new NotImplementedException()
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
        session.subReferenceNodes[family].createRelationshipTo(node, GrailsRelationshipTypes.INSTANCE)
	    LOG.info("created node $node.id with class $family")
        node
    }

    @Override
    protected Object getEntryValue(Object nativeEntry, String property) {
	    def result
	    if (persistentEntity.associations.find { it.name == property } ) {
		    def relname = DynamicRelationshipType.withName(property)

            if (LOG.infoEnabled) {
                nativeEntry.relationships.each {
                    LOG.info("rels $nativeEntry.id  has relationship ${it.startNode.id} -> ${it.endNode.id}, type $it.type")
                }
            }

		    def rel = nativeEntry.getSingleRelationship(relname, Direction.OUTGOING)
		    result = rel ? rel.getOtherNode(nativeEntry).id : null
		    LOG.info("getting property $property via relationship on $nativeEntry = $result")
	    } else {
		    result = nativeEntry.getProperty(property, null)
            def pe = discriminatePersistentEntity(persistentEntity, nativeEntry).getPropertyByName(property)
		    try {
		        result = mappingContext.conversionService.convert(result, pe.type)
            } catch (ConversionException e) {
                LOG.error("prop $property: $e.message")
                throw e
            }
		    LOG.debug("getting property $property on $nativeEntry = $result")
	    }
	    result
    }

	@Override
	protected void setEntryValue(Object nativeEntry, String key, Object value) {
		if (value != null) {
			if (persistentEntity.associations.find { it.name == key } ) {
				LOG.info("setting $key via relationship to $value")

				def relname = DynamicRelationshipType.withName(key)
				def rel = nativeEntry.getSingleRelationship(relname, Direction.OUTGOING)
				if (rel) {
					if (rel.endNode.id == value) {
						return // unchanged value
					}
					rel.delete()
				}

                def targetNodeId = value instanceof Long ? value : value.id
				def targetNode = graphDatabaseService.getNodeById(targetNodeId)
				rel = nativeEntry.createRelationshipTo(targetNode, relname)
                LOG.warn("createRelationship $rel.startNode.id -> $rel.endNode.id ($rel.type)")

			} else {
				LOG.debug("setting property $key = $value ${value?.class}")

				if (!isAllowedNeo4jType(value.class)) {
					value = mappingContext.conversionService.convert(value, String)
				}
				nativeEntry.setProperty(key, value)

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
	        LOG.warn("could not retrieve an Node for id $key")
            null
        }
    }

    @Override
    protected Object storeEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object storeId, Object nativeEntry) {
        assert storeId
        assert nativeEntry
        assert persistentEntity
        storeId // TODO: not sure what to do here...
    }

    @Override
    protected void updateEntry(PersistentEntity persistentEntity, EntityAccess entityAccess, Object key, Object entry) {
        LOG.error("updateentry ")
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void deleteEntries(String family, List keys) {
        LOG.error("delete $keys")
        throw new NotImplementedException()
        //To change body of implemented methods use File | Settings | File Templates.
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
