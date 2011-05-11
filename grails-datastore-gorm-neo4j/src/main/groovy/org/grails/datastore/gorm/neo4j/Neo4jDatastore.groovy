package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.core.AbstractDatastore
import org.springframework.datastore.mapping.core.Session
import org.springframework.beans.factory.InitializingBean
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.kernel.EmbeddedGraphDatabase
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.datastore.mapping.model.MappingContext
import org.springframework.datastore.mapping.model.PersistentEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.Node

/**
 * Datastore implementation for Neo4j backend
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 * TODO: refactor constructors to be groovier
 */
class Neo4jDatastore extends AbstractDatastore implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(Neo4jDatastore.class);

    GraphDatabaseService graphDatabaseService
    def subReferenceNodes // maps entity class names to neo4j subreference node
    def storeDir

    /**
     * only to be called during testing
     * @return
     */
    public Neo4jDatastore() {
        this(new Neo4jMappingContext(), null, null)
    }

    public Neo4jDatastore(Neo4jMappingContext mappingContext, ConfigurableApplicationContext ctx, GraphDatabaseService graphDatabaseService) {
        super(mappingContext, Collections.<String,String>emptyMap(), ctx)
        this.graphDatabaseService = graphDatabaseService


/*        mappingContext.getConverterRegistry().addConverter(new Converter<String, ObjectId>() {
            public ObjectId convert(String source) {
                return new ObjectId(source);
            }
        });

        mappingContext.getConverterRegistry().addConverter(new Converter<ObjectId, String>() {
            public String convert(ObjectId source) {
                return source.toString();
            }
        });*/

    }

    public Neo4jDatastore(Neo4jMappingContext mappingContext, GraphDatabaseService graphDatabaseService) {
        this(mappingContext, null, graphDatabaseService);
    }

    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        new Neo4jSession(this, mappingContext, applicationEventPublisher)
    }

    void afterPropertiesSet() {
        if (!graphDatabaseService) {
            assert storeDir
            graphDatabaseService = new EmbeddedGraphDatabase(storeDir)
        }
        initializeConverters(mappingContext);
        subReferenceNodes = findOrCreateSubReferenceNodes()

    }

    def createSubReferenceNode(name) {
        def tx = graphDatabaseService.beginTx()
        try {
            def subReferenceNode = graphDatabaseService.createNode()
            subReferenceNode.setProperty(Neo4jEntityPersister.SUBREFERENCE_PROPERTY_NAME, name)
            graphDatabaseService.referenceNode.createRelationshipTo(subReferenceNode, GrailsRelationshipTypes.SUBREFERENCE)
            tx.success()
            return subReferenceNode
        } finally {
            tx.finish()
        }
    }

    def findOrCreateSubReferenceNodes() {

        def map = [:]
        Node referenceNode = graphDatabaseService.referenceNode
        for (Relationship rel in referenceNode.getRelationships(GrailsRelationshipTypes.SUBREFERENCE, Direction.OUTGOING)) {
            def endNode = rel.endNode
            def clazz = endNode.getProperty(Neo4jEntityPersister.SUBREFERENCE_PROPERTY_NAME)
            map[clazz] = endNode
        }

        mappingContext.persistentEntities.each {
            if (!map.containsKey(it.name)) {
                def node = createSubReferenceNode(it.name)
                map[it.name] = node
            }
        }
        map
    }

}

