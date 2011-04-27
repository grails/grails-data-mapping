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
import org.springframework.core.convert.ConversionService
import org.codehaus.groovy.runtime.NullObject

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
        //To change body of implemented methods use File | Settings | File Templates.
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
        new Neo4jAssociationIndexer(nativeEntry: nativeEntry, association:association)
    }

    @Override
    protected Object createNewEntry(String family) {
        Node node = graphDatabaseService.createNode()
        node.setProperty(TYPE_PROPERTY_NAME, family)
        session.subReferenceNodes[family].createRelationshipTo(node, GrailsRelationshipTypes.INSTANCE)
        node
    }

    @Override
    protected Object getEntryValue(Object nativeEntry, String property) {
        LOG.info("getting property $property on $nativeEntry")
        nativeEntry.getProperty(property, null)
    }

    @Override
    protected void setEntryValue(Object nativeEntry, String key, Object value) {
        LOG.info("setting property $key = $value ${value?.class}")
        if (value!=null) {
            if (!isAllowedNeo4jType(value.class)) {
                value = mappingContext.conversionService.convert(value, String)
            }
            nativeEntry.setProperty(key, value)
        }
    }

    @Override
    protected Object retrieveEntry(PersistentEntity persistentEntity, String family, Serializable key) {
        try {
            def node = graphDatabaseService.getNodeById(key)
            if (node) {
                assert node.getProperty(TYPE_PROPERTY_NAME) == family
            }
            node
        } catch (NotFoundException e) {
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
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void deleteEntries(String family, List keys) {
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

}
