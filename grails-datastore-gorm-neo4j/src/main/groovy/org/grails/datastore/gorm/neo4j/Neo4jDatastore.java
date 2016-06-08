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

import groovy.lang.Closure;
import org.grails.datastore.gorm.GormEnhancer;
import org.grails.datastore.gorm.events.AutoTimestampEventListener;
import org.grails.datastore.gorm.events.ConfigurableApplicationEventPublisher;
import org.grails.datastore.gorm.events.DefaultApplicationEventPublisher;
import org.grails.datastore.gorm.events.DomainEventListener;
import org.grails.datastore.gorm.neo4j.config.Neo4jDriverConfigBuilder;
import org.grails.datastore.gorm.neo4j.util.EmbeddedNeo4jServer;
import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.core.AbstractDatastore;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.core.StatelessDatastore;
import org.grails.datastore.mapping.graph.GraphDatastore;
import org.grails.datastore.mapping.model.DatastoreConfigurationException;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Simple;
import org.grails.datastore.mapping.reflect.ClassUtils;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.exceptions.Neo4jException;
import org.neo4j.harness.ServerControls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.StandardEnvironment;

import javax.annotation.PreDestroy;
import javax.persistence.FlushModeType;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 * Datastore implementation for Neo4j backend
 *
 * @author Stefan Armbruster (stefan@armbruster-it.de)
 * @author Graeme Rocher
 *
 * @since 1.0
 */
public class Neo4jDatastore extends AbstractDatastore implements Closeable, StatelessDatastore, GraphDatastore {

    public static final String DEFAULT_URL = "bolt://localhost:7687";
    @Deprecated
    public static final String DEFAULT_LOCATION = DEFAULT_URL;
    public static final String SETTING_NEO4J_URL = "grails.neo4j.url";
    public static final String SETTING_NEO4J_BUILD_INDEX = "grails.neo4j.buildIndex";
    public static final String SETTING_NEO4J_LOCATION = "grails.neo4j.location";
    public static final String SETTING_NEO4J_TYPE = "grails.neo4j.type";
    public static final String SETTING_NEO4J_FLUSH_MODE = "grails.neo4j.flush.mode";
    public static final String SETTING_NEO4J_USERNAME = "grails.neo4j.username";
    public static final String SETTING_NEO4J_PASSWORD = "grails.neo4j.password";
    public static final String SETTING_DEFAULT_MAPPING = "grails.neo4j.default.mapping";
    public static final String SETTING_NEO4J_DB_PROPERTIES = "grails.neo4j.options";
    public static final String DEFAULT_DATABASE_TYPE = "remote";
    public static final String DATABASE_TYPE_EMBEDDED = "embedded";

    private static Logger log = LoggerFactory.getLogger(Neo4jDatastore.class);

    protected boolean skipIndexSetup = false;
    protected final Driver boltDriver;
    protected final FlushModeType defaultFlushMode;
    protected final ConfigurableApplicationEventPublisher eventPublisher;
    protected final Neo4jDatastoreTransactionManager transactionManager;
    protected final GormEnhancer gormEnhancer;
    protected static AutoCloseable embeddedServer = null;

    /**
     * Configures a new {@link Neo4jDatastore} for the given arguments
     *
     * @param mappingContext The {@link MappingContext} which contains information about the mapped classes
     * @param configuration The configuration for the datastore
     * @param eventPublisher The Spring ApplicationContext
     */
    public Neo4jDatastore(Driver boltDriver, MappingContext mappingContext, PropertyResolver configuration, ConfigurableApplicationEventPublisher eventPublisher) {
        super(mappingContext, configuration, null);
        this.boltDriver = boltDriver;
        this.eventPublisher = eventPublisher;
        this.defaultFlushMode = configuration.getProperty(SETTING_NEO4J_FLUSH_MODE, FlushModeType.class, FlushModeType.AUTO);
        this.skipIndexSetup = !configuration.getProperty(SETTING_NEO4J_BUILD_INDEX, Boolean.class, true);

        if(!skipIndexSetup) {
            setupIndexing();
        }

        transactionManager = new Neo4jDatastoreTransactionManager(this);
        gormEnhancer = new GormEnhancer(this, transactionManager);

        registerEventListeners(eventPublisher);

        mappingContext.addMappingContextListener(new MappingContext.Listener() {
            @Override
            public void persistentEntityAdded(PersistentEntity entity) {
                gormEnhancer.registerEntity(entity);
            }
        });
    }

    /**
     * Configures a Neo4jDatastore for the given {@link Driver}, {@link MappingContext} and {@link ApplicationEventPublisher}
     *
     * @param boltDriver The Bolt driver
     * @param mappingContext The MappingContext
     */
    public Neo4jDatastore(Driver boltDriver, MappingContext mappingContext) {
        this(boltDriver, mappingContext, new StandardEnvironment(), new DefaultApplicationEventPublisher());
    }

    /**
     * Configures a Neo4jDatastore for the given {@link Driver}, {@link MappingContext} and {@link ApplicationEventPublisher}
     *
     * @param boltDriver The Bolt driver
     * @param mappingContext The MappingContext
     * @param eventPublisher The event publisher
     */
    public Neo4jDatastore(Driver boltDriver, MappingContext mappingContext, ConfigurableApplicationEventPublisher eventPublisher) {
        this(boltDriver, mappingContext, new StandardEnvironment(), eventPublisher);
    }

    /**
     * Configures a new {@link Neo4jDatastore} for the given arguments
     *
     * @param boltDriver The driver
     * @param configuration The configuration for the datastore
     * @param eventPublisher The Spring ApplicationContext
     * @param classes The persistent classes
     */
    public Neo4jDatastore(Driver boltDriver, PropertyResolver configuration, ConfigurableApplicationEventPublisher eventPublisher, Class...classes) {
        this(boltDriver, createMappingContext(configuration,classes), configuration, eventPublisher);
    }

    /**
     * Configures a new {@link Neo4jDatastore} for the given arguments
     *
     * @param boltDriver The driver
     * @param eventPublisher The Spring ApplicationContext
     * @param classes The persistent classes
     */
    public Neo4jDatastore(Driver boltDriver, ConfigurableApplicationEventPublisher eventPublisher, Class...classes) {
        this(boltDriver, createMappingContext(new StandardEnvironment(),classes), new StandardEnvironment(), eventPublisher);
    }
    /**
     * Configures a new {@link Neo4jDatastore} for the given arguments
     *
     * @param boltDriver The driver
     * @param configuration The configuration for the datastore
     * @param classes The persistent classes
     */
    public Neo4jDatastore(Driver boltDriver, PropertyResolver configuration, Class...classes) {
        this(boltDriver, configuration, new DefaultApplicationEventPublisher(), classes);
    }

    /**
     * Configures a new {@link Neo4jDatastore} for the given arguments
     *
     * @param boltDriver The driver
     * @param classes The persistent classes
     */
    public Neo4jDatastore(Driver boltDriver, Class...classes) {
        this(boltDriver, new StandardEnvironment(), new DefaultApplicationEventPublisher(), classes);
    }

    /**
     * Configures a new {@link Neo4jDatastore} for the given arguments
     *
     * @param mappingContext The {@link MappingContext} which contains information about the mapped classes
     * @param configuration The configuration for the datastore
     * @param eventPublisher The Spring ApplicationContext
     */
    public Neo4jDatastore(MappingContext mappingContext, PropertyResolver configuration, ConfigurableApplicationEventPublisher eventPublisher) {
        this(createGraphDatabaseDriver(configuration), mappingContext, configuration, eventPublisher);
    }

    /**
     * Configures a new {@link Neo4jDatastore} for the given arguments
     *
     * @param mappingContext The {@link MappingContext} which contains information about the mapped classes
     * @param configuration The configuration for the datastore
     */
    public Neo4jDatastore(MappingContext mappingContext, PropertyResolver configuration) {
        this(createGraphDatabaseDriver(configuration), mappingContext, configuration, new DefaultApplicationEventPublisher());
    }


    /**
     * Configures a new {@link Neo4jDatastore} for the given arguments
     *
     * @param configuration The configuration for the datastore
     * @param eventPublisher The Spring ApplicationContext
     * @param classes The persistent classes
     */
    public Neo4jDatastore(PropertyResolver configuration, ConfigurableApplicationEventPublisher eventPublisher, Class...classes) {
        this(createGraphDatabaseDriver(configuration), createMappingContext(configuration, classes), configuration, eventPublisher);
    }

    /**
     * Configures a new {@link Neo4jDatastore} for the given arguments
     *
     * @param configuration The configuration for the datastore
     * @param classes The persistent classes
     */
    public Neo4jDatastore(PropertyResolver configuration, Class...classes) {
        this(createGraphDatabaseDriver(configuration), createMappingContext(configuration, classes), configuration, new DefaultApplicationEventPublisher());
    }

    /**
     * Configures a new {@link Neo4jDatastore} for the given arguments
     *
     * @param classes The persistent classes
     */
    public Neo4jDatastore(Class...classes) {
        this(new StandardEnvironment(), classes);
    }

    /**
     * Configures a new {@link Neo4jDatastore} for the given arguments
     *
     * @param configuration The configuration
     * @param classes The persistent classes
     */
    public Neo4jDatastore(Map<String, Object> configuration, Class...classes) {
        this(configuration, new DefaultApplicationEventPublisher(), classes);
    }


    /**
     * Configures a new {@link Neo4jDatastore} for the given arguments
     *
     * @param configuration The configuration
     * @param classes The persistent classes
     */
    public Neo4jDatastore(Map<String, Object> configuration, ConfigurableApplicationEventPublisher eventPublisher,Class...classes) {
        this(createPropertyResolver(configuration),eventPublisher, classes);
    }

    private static PropertyResolver createPropertyResolver(Map<String,Object> configuration) {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("neo4j", configuration));
        return env;
    }

    /**
     * @return The transaction manager
     */
    public Neo4jDatastoreTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public static AutoCloseable getEmbeddedServer() {
        return embeddedServer;
    }

    @Override
    public ConfigurableApplicationEventPublisher getApplicationEventPublisher() {
        return this.eventPublisher;
    }

    protected static Neo4jMappingContext createMappingContext(PropertyResolver configuration, Class[] classes) {
        return new Neo4jMappingContext(configuration.getProperty(SETTING_DEFAULT_MAPPING, Closure.class, null), classes);
    }

    protected static Driver createGraphDatabaseDriver(PropertyResolver configuration) {
        final String url = configuration.getProperty(SETTING_NEO4J_LOCATION, configuration.getProperty(SETTING_NEO4J_URL, (String)null));
        final String username = configuration.getProperty(SETTING_NEO4J_USERNAME, String.class, null);
        final String password = configuration.getProperty(SETTING_NEO4J_PASSWORD, String.class, null);
        final String type = configuration.getProperty(SETTING_NEO4J_TYPE, String.class, DEFAULT_DATABASE_TYPE);
        if(DATABASE_TYPE_EMBEDDED.equalsIgnoreCase(type)) {
            if(ClassUtils.isPresent("org.neo4j.harness.ServerControls") && EmbeddedNeo4jServer.isAvailable()) {
                ServerControls serverControls;
                try {
                    serverControls = url != null ? EmbeddedNeo4jServer.start(url) : EmbeddedNeo4jServer.start();
                } catch (IOException e) {
                    throw new DatastoreConfigurationException("Unable to start embedded Neo4j server: " + e.getMessage(), e);
                }

                embeddedServer = serverControls;
                return GraphDatabase.driver(serverControls.boltURI(), Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig());
            }
            else {
                log.error("Embedded Neo4j server was configured but 'neo4j-harness' classes not found on classpath.");
            }
        }

        AuthToken authToken = null;

        if(username != null && password != null) {
            authToken = AuthTokens.basic(username, password);
        }

        Neo4jDriverConfigBuilder configBuilder = new Neo4jDriverConfigBuilder(configuration);

        return GraphDatabase.driver(url != null ? url : DEFAULT_URL, authToken, configBuilder.build());
    }

    protected void registerEventListeners(ConfigurableApplicationEventPublisher eventPublisher) {
        eventPublisher.addApplicationListener(new DomainEventListener(this));
        eventPublisher.addApplicationListener(new AutoTimestampEventListener(this));
    }

    public void setSkipIndexSetup(boolean skipIndexSetup) {
        this.skipIndexSetup = skipIndexSetup;
    }

    @Override
    protected Session createSession(PropertyResolver connectionDetails) {
        final Neo4jSession neo4jSession = new Neo4jSession(this, mappingContext, eventPublisher, false, boltDriver);
        neo4jSession.setFlushMode(defaultFlushMode);
        return neo4jSession;
    }


    public void setupIndexing() {
        if(skipIndexSetup) return;
        List<String> schemaStrings = new ArrayList<String>(); // using set to avoid duplicate index creation

        for (PersistentEntity persistentEntity:  mappingContext.getPersistentEntities()) {
            if(persistentEntity.isExternal()) continue;

            if(log.isDebugEnabled()) {
                log.debug("Setting up indexing for entity " + persistentEntity.getName());
            }
            final GraphPersistentEntity graphPersistentEntity = (GraphPersistentEntity) persistentEntity;
            for (String label: graphPersistentEntity.getLabels()) {
                StringBuilder sb = new StringBuilder();
                if(graphPersistentEntity.getIdGenerator() != null) {
                    sb.append("CREATE CONSTRAINT ON (n:").append(label).append(") ASSERT n.").append(CypherBuilder.IDENTIFIER).append(" IS UNIQUE");
                    schemaStrings.add(sb.toString());
                }



                for (PersistentProperty persistentProperty : persistentEntity.getPersistentProperties()) {
                    Property mappedForm = persistentProperty.getMapping().getMappedForm();
                    if ((persistentProperty instanceof Simple) && (mappedForm != null) ) {

                        if(mappedForm.isUnique()) {
                            sb = new StringBuilder();

                            sb.append("CREATE CONSTRAINT ON (n:").append(label).append(") ASSERT n.").append(persistentProperty.getName()).append(" IS UNIQUE");
                            schemaStrings.add(sb.toString());
                        }
                        else if(mappedForm.isIndex()) {
                            sb = new StringBuilder();
                            sb.append("CREATE INDEX ON :").append(label).append("(").append(persistentProperty.getName()).append(")");
                            schemaStrings.add(sb.toString());
                            if(log.isDebugEnabled()) {
                                log.debug("setting up indexing for " + label + " property " + persistentProperty.getName());
                            }
                        }
                    }
                }
            }
        }

        org.neo4j.driver.v1.Session boltSession = boltDriver.session();

        final Transaction transaction = boltSession.beginTransaction();;
        try {
            for (String cypher: schemaStrings) {
                if(log.isDebugEnabled()) {
                    log.debug("CREATE INDEX Cypher [{}]", cypher);
                }
                transaction.run(cypher);
                transaction.success();
            }
        } catch(Throwable e) {
            log.error("Error creating Neo4j index: " + e.getMessage(), e);
            transaction.failure();
            throw new DatastoreConfigurationException("Error creating Neo4j index: " + e.getMessage(), e);
        }
        finally {
            transaction.close();
        }
        if(log.isDebugEnabled()) {
            log.debug("done setting up indexes");
        }
    }

    /**
     * @return The {@link Driver} used by this datastore
     */
    public Driver getBoltDriver() {
        return boltDriver;
    }

    @Override
    @PreDestroy
    public void close() throws IOException {
        try {
            try {
                gormEnhancer.close();
            } catch (Throwable e) {
                // ignore
            }
            try {
                if(boltDriver != null) {
                    boltDriver.close();
                }
            } catch (Neo4jException e) {
                log.error("Error shutting down Bolt driver: " + e.getMessage(), e);
            }
            if(embeddedServer != null) {
                try {
                    embeddedServer.close();
                } catch (Throwable e) {
                    log.error("Error shutting down Embedded Neo4j server: " + e.getMessage(), e);
                }
                finally {
                    embeddedServer = null;
                }
            }
            super.destroy();
        } catch (Exception e) {
            throw new IOException("Error shutting down Neo4j datastore", e);
        }
    }
}