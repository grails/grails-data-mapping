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

import org.grails.datastore.mapping.core.AbstractDatastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.PersistentEntity
import org.neo4j.kernel.EmbeddedGraphDatabase
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ConfigurableApplicationContext
import org.neo4j.graphdb.*
import org.springframework.util.Assert
import org.neo4j.cypher.javacompat.ExecutionEngine
import org.neo4j.graphdb.index.IndexManager
import org.grails.datastore.mapping.model.PersistentProperty
import org.neo4j.graphdb.index.AutoIndexer
import org.grails.datastore.mapping.model.types.Simple

/**
 * Datastore implementation for Neo4j backend
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 * TODO: refactor constructors to be groovier
 */
class Neo4jDatastore extends AbstractDatastore implements InitializingBean {

    public static final NUMBER_OF_SUBSUBREFERENCE_NODES = 2 ** 7

    GraphDatabaseService graphDatabaseService
    @Lazy ExecutionEngine executionEngine = new ExecutionEngine(graphDatabaseService)
    Map<Class, Node> subReferenceNodes
    Map<Node, Map<Integer, Node>> subSubReferenceNodes
    String storeDir
    @Lazy IndexManager indexManager = graphDatabaseService.index()
    Map<PersistentEntity, Collection<PersistentEntity>> domainSubclasses = [:].withDefault { [] }

    /**
     * only to be called during testing
     * @return
     */
    Neo4jDatastore() {
        this(new Neo4jMappingContext(), null, null)
    }

    Neo4jDatastore(Neo4jMappingContext mappingContext, ConfigurableApplicationContext ctx, GraphDatabaseService graphDatabaseService) {
        super(mappingContext, Collections.<String,String>emptyMap(), ctx)
        this.graphDatabaseService = graphDatabaseService

/*        mappingContext.getConverterRegistry().addConverter(new Converter<String, ObjectId>() {
            ObjectId convert(String source) {
                return new ObjectId(source)
            }
        })

        mappingContext.getConverterRegistry().addConverter(new Converter<ObjectId, String>() {
            String convert(ObjectId source) {
                return source.toString()
            }
        })*/

    }

    Neo4jDatastore(Neo4jMappingContext mappingContext, GraphDatabaseService graphDatabaseService) {
        this(mappingContext, null, graphDatabaseService)
    }

    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        new Neo4jSession(
                datastore: this,
                mappingContext: mappingContext,
                applicationEventPublisher: applicationEventPublisher
        )
    }

    void afterPropertiesSet() {
        if (!graphDatabaseService) {
            Assert.notNull storeDir
            graphDatabaseService = new EmbeddedGraphDatabase(storeDir)
        }
        initializeConverters(mappingContext)
        findOrCreateSubReferenceNodes()
        setupIndexing()
    }

    protected void setupIndexing() {
        AutoIndexer<Node> nodeAutoIndex = indexManager.nodeAutoIndexer
        nodeAutoIndex.enabled = true
        nodeAutoIndex.startAutoIndexingProperty(Neo4jSession.TYPE_PROPERTY_NAME)
        for (PersistentEntity pe in mappingContext.persistentEntities) {

            for (PersistentEntity parent in collectSuperclassChain(pe)) {
                domainSubclasses[parent] << pe
            }

            for (PersistentProperty pp in pe.persistentProperties) {
                if ((pp instanceof Simple) && (pp.mapping.mappedForm.index)) {
                    nodeAutoIndex.startAutoIndexingProperty(pp.name)
                }
            }
        }
    }

    protected List collectSuperclassChain(PersistentEntity pe, def list=[]) {
        if (pe) {
            list << pe
            return collectSuperclassChain(pe.parentEntity, list)
        }
        list
    }

    protected void findOrCreateSubReferenceNodes() {
        Transaction tx = graphDatabaseService.beginTx()
        try {
            subReferenceNodes = [:]
            Node referenceNode = graphDatabaseService.referenceNode

            for (Relationship rel in referenceNode.getRelationships(GrailsRelationshipTypes.SUBREFERENCE, Direction.OUTGOING).iterator()) {
                Node endNode = rel.endNode
                String clazzName = endNode.getProperty(Neo4jSession.SUBREFERENCE_PROPERTY_NAME)
                subReferenceNodes[clazzName] = endNode
            }

            for (PersistentEntity pe in mappingContext.persistentEntities) {
                if (!subReferenceNodes.containsKey(pe.name)) {
                    subReferenceNodes[pe.name] = createSubReferenceNode(pe.name)
                }
            }
            tx.success()
        } finally {
            tx.finish()
        }
    }

    protected Node createSubReferenceNode(name) {
            Node subReferenceNode = graphDatabaseService.createNode()
            subReferenceNode.setProperty(Neo4jSession.SUBREFERENCE_PROPERTY_NAME, name)
            graphDatabaseService.referenceNode.createRelationshipTo(subReferenceNode, GrailsRelationshipTypes.SUBREFERENCE)
            return subReferenceNode
    }

}