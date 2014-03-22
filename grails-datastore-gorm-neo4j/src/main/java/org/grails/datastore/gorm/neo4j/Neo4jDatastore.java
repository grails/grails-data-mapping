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
package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.gorm.neo4j.engine.CypherEngine;
import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.core.AbstractDatastore;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Simple;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Datastore implementation for Neo4j backend
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
public class Neo4jDatastore extends AbstractDatastore implements InitializingBean {

    private static Logger log = LoggerFactory.getLogger(Neo4jDatastore.class);

    protected MappingContext mappingContext;
    protected CypherEngine cypherEngine;
    protected GraphDatabaseService graphDatabaseService;
    protected AtomicLong atomicIdCounter;
    protected boolean skipIndexSetup = false;

    public Neo4jDatastore(MappingContext mappingContext, ApplicationContext applicationContext, CypherEngine cypherEngine, GraphDatabaseService graphDatabaseService) {
        super(mappingContext);
        this.mappingContext = mappingContext;
        this.cypherEngine = cypherEngine;
        this.graphDatabaseService = graphDatabaseService;
        setApplicationContext(applicationContext);
    }

    public void setSkipIndexSetup(boolean skipIndexSetup) {
        this.skipIndexSetup = skipIndexSetup;
    }

    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        return new Neo4jSession(this, mappingContext, getApplicationContext(), false, cypherEngine, graphDatabaseService);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!skipIndexSetup) {
            setupIndexing();
        }
        loadIdCounters();
    }

    private void loadIdCounters() {

        Transaction tx = graphDatabaseService.beginTx(); // TODO: use some declarative approach
        try {
            Long idCounter = (Long) IteratorUtil.single(cypherEngine.execute("MERGE (n:__IdCounter__) ON CREATE set n.idCounter=0 RETURN n.idCounter")).get("n.idCounter");
            atomicIdCounter = new AtomicLong(idCounter);
            tx.success();
        } finally {
            tx.close();
        }
    }

    public long nextIdForType(PersistentEntity pe) {
        return atomicIdCounter.incrementAndGet();
    }

    public void setupIndexing() {
        for (PersistentEntity persistentEntity:  mappingContext.getPersistentEntities()) {
            StringBuilder sb = new StringBuilder();
            String label = persistentEntity.getDiscriminator();
            sb.append("CREATE INDEX ON :").append(label).append("(__id__)");
            cypherEngine.execute(sb.toString());
            for (PersistentProperty persistentProperty: persistentEntity.getPersistentProperties()) {
                Property mappedForm = persistentProperty.getMapping().getMappedForm();
                if ((persistentProperty instanceof Simple) && (mappedForm !=null) && (mappedForm.isIndex())) {
                    sb = new StringBuilder();
                    sb.append("CREATE INDEX ON :").append(label).append("(").append(persistentProperty.getName()).append(")");
                    cypherEngine.execute(sb.toString());
                    log.debug("setting up indexing for " + label + " property " + persistentProperty.getName());
                }
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
        cypherEngine.execute("MERGE (n:__IdCounter__) SET n.idCounter={idCounter}", Collections.singletonMap("idCounter", atomicIdCounter.get()));
    }
}