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
import org.grails.datastore.mapping.core.StatelessDatastore;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Datastore implementation for Neo4j backend
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
public class Neo4jDatastore extends AbstractDatastore implements InitializingBean, DisposableBean, StatelessDatastore {

    private static Logger log = LoggerFactory.getLogger(Neo4jDatastore.class);

    protected MappingContext mappingContext;
    protected CypherEngine cypherEngine;
    protected boolean skipIndexSetup = false;

    protected IdGenerator idGenerator = new SnowflakeIdGenerator();

    public Neo4jDatastore(MappingContext mappingContext, ApplicationContext applicationContext, CypherEngine cypherEngine) {
        super(mappingContext);
        this.mappingContext = mappingContext;
        this.cypherEngine = cypherEngine;
        setApplicationContext(applicationContext);
    }

    public void setSkipIndexSetup(boolean skipIndexSetup) {
        this.skipIndexSetup = skipIndexSetup;
    }

    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        return new Neo4jSession(this, mappingContext, getApplicationContext(), false, cypherEngine);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!skipIndexSetup) {
            setupIndexing();
        }
    }

    public long nextIdForType(PersistentEntity pe) {
        return idGenerator.nextId();
    }

    public void setupIndexing() {
        Set<String> schemaStrings = new HashSet<String>(); // using set to avoid duplicate index creation

        for (PersistentEntity persistentEntity:  mappingContext.getPersistentEntities()) {
            for (String label: ((GraphPersistentEntity)persistentEntity).getLabels()) {
                StringBuilder sb = new StringBuilder();
                sb.append("CREATE INDEX ON :").append(label).append("(__id__)");
                schemaStrings.add(sb.toString());
                for (PersistentProperty persistentProperty : persistentEntity.getPersistentProperties()) {
                    Property mappedForm = persistentProperty.getMapping().getMappedForm();
                    if ((persistentProperty instanceof Simple) && (mappedForm != null) && (mappedForm.isIndex())) {
                        sb = new StringBuilder();
                        sb.append("CREATE INDEX ON :").append(label).append("(").append(persistentProperty.getName()).append(")");
                        schemaStrings.add(sb.toString());
                        log.debug("setting up indexing for " + label + " property " + persistentProperty.getName());
                    }
                }
            }
        }

        for (String cypher: schemaStrings) {
            cypherEngine.execute(cypher);
        }
        cypherEngine.commit();
    }
}