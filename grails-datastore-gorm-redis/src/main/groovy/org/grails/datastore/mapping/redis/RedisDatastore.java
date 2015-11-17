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
package org.grails.datastore.mapping.redis;

import static org.grails.datastore.mapping.config.utils.ConfigUtils.read;

import java.util.*;

import org.grails.datastore.mapping.config.utils.PropertyResolverMap;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.core.AbstractDatastore;
import org.grails.datastore.mapping.core.DatastoreUtils;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.engine.PropertyValueIndexer;
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.redis.engine.RedisEntityPersister;
import org.grails.datastore.mapping.redis.util.JedisTemplate;
import org.grails.datastore.mapping.redis.util.RedisTemplate;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ClassUtils;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * A Datastore implementation for the Redis key/value datastore.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class RedisDatastore extends AbstractDatastore implements InitializingBean, DisposableBean {

    private static final boolean jedisClientAvailable =
            ClassUtils.isPresent("redis.clients.jedis.Jedis", RedisSession.class.getClassLoader());

    public static final String CONFIG_HOST = "grails.gorm.redis.host";
    public static final String CONFIG_TIMEOUT = "grails.gorm.redis.timeout";
    public static final String CONFIG_RESOURCE_COUNT = "grails.gorm.redis.resources";
    public static final String CONFIG_PORT = "grails.gorm.redis.port";
    public static final String CONFIG_PASSWORD = "grails.gorm.redis.password";
    public static final String CONFIG_POOLED = "grails.gorm.redis.pooled";

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 6379;
    private String host = DEFAULT_HOST;
    private String password;
    private int port = DEFAULT_PORT;

    private int timeout = 2000;
    private boolean pooled = true;
    private boolean backgroundIndex;
    private JedisPool pool;

    public RedisDatastore() {
        this(new KeyValueMappingContext(""));
    }

    public RedisDatastore(MappingContext mappingContext) {
        this(mappingContext, Collections.<String,Object>emptyMap(), null);
    }

    public RedisDatastore(MappingContext mappingContext, Map<String, Object> config) {
        this(mappingContext, convertToResolver(config), null);
    }

    public RedisDatastore(MappingContext mappingContext, Map<String, Object> config, ConfigurableApplicationContext applicationContext) {
        this(mappingContext, convertToResolver(config), applicationContext);
    }

    public RedisDatastore(MappingContext mappingContext, PropertyResolver configuration,
                ConfigurableApplicationContext ctx) {
        super(mappingContext, new PropertyResolverMap(configuration), ctx);

        int resourceCount = 10;
        if (configuration != null) {
            host = configuration.getProperty(CONFIG_HOST, DEFAULT_HOST);
            port = configuration.getProperty(CONFIG_PORT, Integer.class, DEFAULT_PORT);
            timeout = configuration.getProperty(CONFIG_TIMEOUT, Integer.class, 2000);
            pooled = configuration.getProperty(CONFIG_POOLED, Boolean.class, true);
            password = configuration.getProperty(CONFIG_PASSWORD, (String)null);
            resourceCount = configuration.getProperty(CONFIG_RESOURCE_COUNT, Integer.class, resourceCount);
        }
        if (pooled && useJedis()) {
            this.pool = JedisTemplateFactory.createPool(host, port, timeout, resourceCount, password);
        }

        initializeConverters(mappingContext);
    }

    private static PropertyResolver convertToResolver(Map<String, Object> config) {
        if(config instanceof PropertyResolver) {
            return (PropertyResolver) config;
        }
        else {
            StandardEnvironment env = new StandardEnvironment();
            env.getPropertySources().addFirst(new MapPropertySource("datastoreConfig", config));
            return env;
        }
    }

    private boolean useJedis() {
        return jedisClientAvailable;
    }

    public void destroy() throws Exception {
        super.destroy();
        if (pool != null) {
            pool.destroy();
        }
    }

    static class JedisTemplateFactory {
        static JedisPool pool;

        static JedisPool createPool(String host, int port, int timeout, int resources, String password) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(resources);
            poolConfig.setMaxWaitMillis(timeout);

            poolConfig.setMaxIdle(5);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setNumTestsPerEvictionRun(10);
            poolConfig.setTimeBetweenEvictionRunsMillis(60000);

            if (password != null) {
                pool = new JedisPool(poolConfig, host, port, timeout, password);
            }
            else {
                pool = new JedisPool(poolConfig, host, port, timeout);
            }
            return pool;
        }

        static RedisTemplate create(String host, int port, int timeout, boolean pooled, String password) {

            JedisTemplate template;

            if (pooled) {
                template = new JedisTemplate(pool);
            }
            else {
                template = new JedisTemplate(host, port, timeout);
            }

            if (password != null) {
                template.setPassword(password);
            }

            return template;
        }
    }

    /**
     * Sets whether the Redis datastore should create indices in the background instead of on startup
     *
     * @param backgroundIndex True to create indices in the background
     */
    public void setBackgroundIndex(boolean backgroundIndex) {
        this.backgroundIndex = backgroundIndex;
    }

    @Override
    protected Session createSession(Map<String, String> connDetails) {
        if (!useJedis()) {
            throw new IllegalStateException(
                    "Cannot create RedisSession. No Redis client library found on classpath. " +
                    "Please make sure you have the Jedis library on your classpath");
        }

        return new RedisSession(this, getMappingContext(),
            JedisTemplateFactory.create(host, port, timeout, pooled, password),
            getApplicationEventPublisher());
    }

    public void afterPropertiesSet() throws Exception {
        if (backgroundIndex) {
            new Thread(new IndexOperation()).start();
        }
        else {
            new IndexOperation().run();
        }
    }

    /**
     * Used to update indices on startup or when a new entity is added
     */
    class IndexOperation implements Runnable {

        public void run() {
            final Session session = RedisDatastore.this.connect();
            try {

                DatastoreUtils.bindSession(session);

                final Collection<PersistentEntity> entities = RedisDatastore.this.getMappingContext().getPersistentEntities();
                for (PersistentEntity entity : entities) {
                    final List<PersistentProperty> props = entity.getPersistentProperties();
                    List<PersistentProperty> indexed = new ArrayList<PersistentProperty>();
                    for (PersistentProperty prop : props) {
                        Property kv = (Property) prop.getMapping().getMappedForm();
                        if (kv != null && kv.isIndex()) {
                            indexed.add(prop);
                        }
                    }

                    if (!indexed.isEmpty()) {
                        // page through entities indexing each one
                        final Class cls = entity.getJavaClass();
                        Query query = session.createQuery(cls);
                        query.projections().count();
                        Long total = (Long) query.singleResult();

                        if (total < 100) {
                            List persistedObjects = session.createQuery(cls).list();
                            for (Object persistedObject : persistedObjects) {
                                updatedPersistedObjectIndices(session, entity, persistedObject, indexed);
                            }
                        }
                        else {
                            query = session.createQuery(cls);
                            int offset = 0;
                            int max = 100;

                            // 300+100 < 350
                            while(offset < total) {
                                query.offset(offset);
                                query.max(max);
                                List persistedObjects = query.list();
                                for (Object persistedObject : persistedObjects) {
                                    updatedPersistedObjectIndices(session, entity, persistedObject, indexed);
                                }

                                offset += max;
                            }
                        }
                    }
                }
            }
            finally {
                session.disconnect();
            }
        }

        private void updatedPersistedObjectIndices(Session session, PersistentEntity entity, Object persistedObject, List<PersistentProperty> indexed) {
            EntityAccess ea = ((RedisSession)session).createEntityAccess(entity, persistedObject);
            Object identifier = ea.getIdentifier();
            for (PersistentProperty persistentProperty : indexed) {
                Object value = ea.getProperty(persistentProperty.getName());
                if (value != null) {
                    RedisEntityPersister persister = (RedisEntityPersister) session.getPersister(entity.getJavaClass());
                    PropertyValueIndexer indexer = persister.getPropertyIndexer(persistentProperty);
                    indexer.index(value, identifier);
                }
            }
        }
    }
}
