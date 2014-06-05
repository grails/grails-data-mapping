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
package org.grails.datastore.mapping.cassandra;

import static org.grails.datastore.mapping.config.utils.ConfigUtils.read;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.grails.datastore.gorm.cassandra.mapping.CassandraGormSimpleTypeHolder;
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext;
import org.grails.datastore.mapping.core.AbstractDatastore;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cassandra.config.CassandraCqlClusterFactoryBean;
import org.springframework.cassandra.core.SessionCallback;
import org.springframework.cassandra.core.cql.generator.CreateIndexCqlGenerator;
import org.springframework.cassandra.core.keyspace.CreateIndexSpecification;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.convert.CassandraConverter;
import org.springframework.data.cassandra.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.mapping.BasicCassandraMappingContext;
import org.springframework.data.cassandra.mapping.BasicCassandraPersistentProperty;
import org.springframework.data.cassandra.mapping.CassandraPersistentEntity;
import org.springframework.data.cassandra.mapping.CassandraPersistentProperty;
import org.springframework.data.cassandra.mapping.CassandraSimpleTypeHolder;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.util.Assert;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;

public class CassandraDatastore extends AbstractDatastore implements InitializingBean, DisposableBean, MappingContext.Listener {

    private static Logger log = LoggerFactory.getLogger(CassandraDatastore.class);
    // TODO make one keyspace for each session somehow, maybe just do a
    // different datastore instance?
    public static final String DEFAULT_KEYSPACE = "CassandraKeySpace";
    public static final SchemaAction DEFAULT_SCHEMA_ACTION = SchemaAction.NONE;
    public static final String CASSANDRA_CONTACT_POINTS = "contactPoints";
    public static final String CASSANDRA_PORT = "port";
    public static final String CASSANDRA_KEYSPACE = "keyspace";
    public static final String CASSANDRA_SCHEMA_ACTION = "schemaAction";

    protected Cluster nativeCluster;
    protected com.datastax.driver.core.Session nativeSession;
    protected BasicCassandraMappingContext springCassandraMappingContext;
    protected CassandraTemplate cassandraTemplate;
    protected boolean stateless = false;
    protected String keyspace;

    public CassandraDatastore() {
        this(new CassandraMappingContext(), Collections.<String, String>emptyMap(), null);
    }
    
    public CassandraDatastore(Map<String, String> connectionDetails, ConfigurableApplicationContext ctx) {
        this(new CassandraMappingContext(), connectionDetails, ctx);
    }

    public CassandraDatastore(CassandraMappingContext mappingContext, Map<String, String> connectionDetails, ConfigurableApplicationContext ctx) {
        super(mappingContext, connectionDetails, ctx);
        this.keyspace = mappingContext.getKeyspace();                
        springCassandraMappingContext = new BasicCassandraMappingContext() {
            @Override
            public CassandraPersistentProperty createPersistentProperty(Field field, PropertyDescriptor descriptor, CassandraPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
                if (field != null && Modifier.isTransient(field.getModifiers())) {
                    return new BasicCassandraPersistentProperty(field, descriptor, owner, (CassandraSimpleTypeHolder) simpleTypeHolder) {
                        public boolean isTransient() { return true ;}
                    };
                }
                return super.createPersistentProperty(field, descriptor, owner, simpleTypeHolder);
            }
        };
        springCassandraMappingContext.setSimpleTypeHolder(new CassandraGormSimpleTypeHolder());
        
        mappingContext.setSpringCassandraMappingContext(springCassandraMappingContext);
        if (mappingContext != null) {
            mappingContext.addMappingContextListener(this);
        }

        initializeConverters(mappingContext);
        log.debug("Initializing Cassandra Datastore for keyspace: " + keyspace);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        createCluster();
        createNativeSession();
    }

    public Cluster createCluster() throws Exception {
        if (nativeCluster == null) {
            CassandraCqlClusterFactoryBean cassandraClusterFactory = new CassandraCqlClusterFactoryBean();
            cassandraClusterFactory.setContactPoints(read(String.class, CASSANDRA_CONTACT_POINTS, connectionDetails, CassandraCqlClusterFactoryBean.DEFAULT_CONTACT_POINTS));
            cassandraClusterFactory.setPort(read(Integer.class, CASSANDRA_PORT, connectionDetails, CassandraCqlClusterFactoryBean.DEFAULT_PORT));
            cassandraClusterFactory.afterPropertiesSet();
            nativeCluster = cassandraClusterFactory.getObject();
        }
        return nativeCluster;
    }

    public com.datastax.driver.core.Session createNativeSession() throws ClassNotFoundException, Exception {
        if (nativeSession == null) {
            Assert.notNull(nativeCluster);
            Assert.notNull(keyspace);
            GormCassandraSessionFactoryBean cassandraSessionFactory = new GormCassandraSessionFactoryBean();
            cassandraSessionFactory.setCluster(nativeCluster);
            cassandraSessionFactory.setKeyspaceName(this.keyspace);
            MappingCassandraConverter mappingCassandraConverter = new MappingCassandraConverter(cassandraMapping());
            DefaultConversionService conversionService = (DefaultConversionService) mappingCassandraConverter.getConversionService();
            conversionService.addConverter(new Converter<URL, String>() {
               
                @Override
                public String convert(URL source) {
                    return source.toString();
                }
            });
            cassandraSessionFactory.setConverter(mappingCassandraConverter);
            cassandraSessionFactory.setSchemaAction(read(SchemaAction.class, CASSANDRA_SCHEMA_ACTION, connectionDetails, DEFAULT_SCHEMA_ACTION));
            // TODO: startup and shutdown scripts addition
            cassandraSessionFactory.afterPropertiesSet();
            nativeSession = cassandraSessionFactory.getObject();
            cassandraTemplate = new CassandraTemplate(nativeSession, mappingCassandraConverter);
        }
        return nativeSession;
    }

    public org.springframework.data.cassandra.mapping.CassandraMappingContext cassandraMapping() throws ClassNotFoundException {

        Collection<PersistentEntity> persistentEntities = mappingContext.getPersistentEntities();
        Set<Class<?>> entitySet = new HashSet<Class<?>>();
        for (PersistentEntity persistentEntity : persistentEntities) {
            entitySet.add(persistentEntity.getJavaClass());
        }
        springCassandraMappingContext.setInitialEntitySet(entitySet);
        springCassandraMappingContext.afterPropertiesSet();

        return springCassandraMappingContext;
    }

    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        if (stateless) {
            return createStatelessSession(connectionDetails);
        } else {
            return new CassandraSession(this, getMappingContext(), this.nativeSession, getApplicationEventPublisher(), false, cassandraTemplate);
        }
    }

    @Override
    protected Session createStatelessSession(Map<String, String> connectionDetails) {
        return new CassandraSession(this, getMappingContext(), this.nativeSession, getApplicationEventPublisher(), true, cassandraTemplate);
    }

    @Override
    public void persistentEntityAdded(PersistentEntity entity) {
        // get adds persistententity
        springCassandraMappingContext.getPersistentEntity(entity.getJavaClass());
    }

    public Cluster getNativeCluster() {
        return nativeCluster;
    }

    public com.datastax.driver.core.Session getNativeSession() {
        return nativeSession;
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
        nativeSession.close();
        nativeCluster.close();
    }

    public static class GormCassandraConverter extends MappingCassandraConverter implements CassandraConverter {
        public GormCassandraConverter() {

        }
    }

    // TODO: replace index creation when spring-data-cassandra has implemented
    // it
    private static class GormCassandraSessionFactoryBean extends CassandraSessionFactoryBean {
        @Override
        public void afterPropertiesSet() throws Exception {
            super.afterPropertiesSet();
            performIndexAction();
        }

        private void performIndexAction() {
            switch (schemaAction) {

            case NONE:
                return;

            case RECREATE_DROP_UNUSED:
                // don't break!
            case RECREATE:
                // don't break!
            case CREATE:
                createIndex();
            }

        }

        private void createIndex() {
            Collection<? extends CassandraPersistentEntity<?>> entities = converter.getMappingContext().getNonPrimaryKeyEntities();
            for (final CassandraPersistentEntity<?> entity : entities) {
                entity.doWithProperties(new PropertyHandler<CassandraPersistentProperty>() {
                    @Override
                    public void doWithPersistentProperty(CassandraPersistentProperty persistentProperty) {
                        if (persistentProperty.isIndexed()) {
                            final CreateIndexSpecification createIndexSpecification = new CreateIndexSpecification();
                            createIndexSpecification.tableName(entity.getTableName()).columnName(persistentProperty.getColumnName()).ifNotExists();
                            admin.execute(new SessionCallback<ResultSet>() {
                                @Override
                                public ResultSet doInSession(com.datastax.driver.core.Session s) throws DataAccessException {
                                    String cql = CreateIndexCqlGenerator.toCql(createIndexSpecification);
                                    log.debug(cql);
                                    return s.execute(cql);
                                }
                            });
                        }
                    }
                });
            }
        }
    }
}
