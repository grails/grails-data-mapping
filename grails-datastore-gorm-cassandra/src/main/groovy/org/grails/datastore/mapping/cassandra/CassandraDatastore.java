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

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.convert.CassandraConverter;
import org.springframework.data.cassandra.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.mapping.BasicCassandraMappingContext;
import org.springframework.util.Assert;

import com.datastax.driver.core.Cluster;

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
    protected BasicCassandraMappingContext cassandraMappingContext;
    protected CassandraTemplate cassandraTemplate;
    protected boolean stateless = false;

    public CassandraDatastore(Map<String, String> connectionDetails, ConfigurableApplicationContext ctx) {
        this(new CassandraMappingContext(CassandraDatastore.DEFAULT_KEYSPACE), connectionDetails, ctx);
    }

    public CassandraDatastore(MappingContext mappingContext, Map<String, String> connectionDetails, ConfigurableApplicationContext ctx) {
        super(mappingContext, connectionDetails, ctx);
        cassandraMappingContext = new BasicCassandraMappingContext();

        if (mappingContext != null) {
            mappingContext.addMappingContextListener(this);
        }

        initializeConverters(mappingContext);

        mappingContext.getConverterRegistry().addConverter(new Converter<Date, Calendar>() {
            public Calendar convert(Date source) {
                Calendar dest = Calendar.getInstance();
                dest.setTime(source);
                return dest;
            }
        });

        mappingContext.getConverterRegistry().addConverter(new Converter<Calendar, Date>() {
            public Date convert(Calendar source) {
                return source.getTime();
            }
        });

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
            CassandraSessionFactoryBean cassandraSessionFactory = new CassandraSessionFactoryBean();
            cassandraSessionFactory.setCluster(nativeCluster);
            cassandraSessionFactory.setKeyspaceName(read(String.class, CASSANDRA_KEYSPACE, connectionDetails, DEFAULT_KEYSPACE));
            MappingCassandraConverter mappingCassandraConverter = new MappingCassandraConverter(cassandraMapping());
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
        cassandraMappingContext.setInitialEntitySet(entitySet);
        cassandraMappingContext.afterPropertiesSet();

        return cassandraMappingContext;
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
        cassandraMappingContext.getPersistentEntity(entity.getJavaClass());
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

}
