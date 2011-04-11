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
package org.springframework.datastore.mapping.redis;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.datastore.mapping.config.Property;
import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.engine.EntityAccess;
import org.springframework.datastore.mapping.engine.PropertyValueIndexer;
import org.springframework.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.query.Query;
import org.springframework.datastore.mapping.redis.engine.RedisEntityPersister;
import org.springframework.datastore.mapping.redis.util.JedisTemplate;
import org.springframework.datastore.mapping.redis.util.RedisTemplate;
import org.springframework.util.ClassUtils;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.springframework.datastore.mapping.config.utils.ConfigUtils.read;

/**
 * A Datastore implementation for the Redis key/value datastore
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisDatastore extends AbstractDatastore implements InitializingBean, DisposableBean {

    private static final boolean jedisClientAvailable =
            ClassUtils.isPresent("redis.clients.jedis.Jedis", RedisSession.class.getClassLoader());

    public static final String CONFIG_HOST = "host";
    public static final String CONFIG_TIMEOUT = "timeout";
    private static final String CONFIG_RESOURCE_COUNT = "resources";
    public static final String CONFIG_PORT = "port";
    public static final String CONFIG_PASSWORD = "password";

    private static final String CONFIG_POOLED = "pooled";

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
        this(mappingContext, null, null);
    }

    public RedisDatastore(MappingContext mappingContext, Map<String, String> connectionDetails,
                ConfigurableApplicationContext ctx) {
        super(mappingContext, connectionDetails, ctx);

        int resourceCount = 10;
        if (connectionDetails != null) {
            host = read(String.class, CONFIG_HOST, connectionDetails, DEFAULT_HOST);
            port = read(Integer.class, CONFIG_PORT, connectionDetails, DEFAULT_PORT);
            timeout = read(Integer.class, CONFIG_TIMEOUT, connectionDetails, 2000);
            pooled = read(Boolean.class, CONFIG_POOLED, connectionDetails, true);
            password = read(String.class, CONFIG_PASSWORD, connectionDetails, null);
            resourceCount = read(Integer.class, CONFIG_RESOURCE_COUNT, connectionDetails, resourceCount);
        }
        if (pooled && useJedis()) {
            this.pool = JedisTemplateFactory.createPool(host, port, timeout, resourceCount, password);
        }

        initializeConverters(mappingContext);
    }

    private boolean useJedis() {
        return jedisClientAvailable;
    }

    public void destroy() throws Exception {
        if (pool != null) {
            pool.destroy();
        }
    }

    static class JedisTemplateFactory {
        static JedisPool pool;

        static JedisPool createPool(String host, int port, int timeout, int resources, String password) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxWait(timeout);
            poolConfig.setMaxActive(resources);
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

        private void updatedPersistedObjectIndices(Session session, PersistentEntity entity, Object persistedObject, List<PersistentProperty> indexed) {
            EntityAccess ea = new EntityAccess(entity, persistedObject);
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
