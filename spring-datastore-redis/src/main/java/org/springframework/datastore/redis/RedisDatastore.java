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
package org.springframework.datastore.redis;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.datastore.core.AbstractDatastore;
import org.springframework.datastore.core.Session;
import org.springframework.datastore.engine.EntityAccess;
import org.springframework.datastore.engine.PropertyValueIndexer;
import org.springframework.datastore.keyvalue.mapping.KeyValue;
import org.springframework.datastore.keyvalue.mapping.KeyValueMappingContext;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.query.Query;
import org.springframework.datastore.redis.engine.RedisEntityPersister;
import org.springframework.datastore.redis.util.JedisTemplate;
import org.springframework.datastore.redis.util.RedisClientTemplate;
import org.springframework.datastore.redis.util.RedisTemplate;
import org.springframework.util.ClassUtils;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.concurrent.TimeoutException;

import static org.springframework.datastore.config.utils.ConfigUtils.read;

/**
 * A Datastore implementation for the Redis key/value datastore
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisDatastore extends AbstractDatastore implements InitializingBean {

    private static final boolean redisClientAvailable =
            ClassUtils.isPresent("sma.RedisClient", RedisSession.class.getClassLoader());

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
    private boolean pooled;
    private boolean backgroundIndex;

    public RedisDatastore() {
        this(new KeyValueMappingContext(""));
    }

    public RedisDatastore(MappingContext mappingContext) {
        this(mappingContext, null);
    }

    public RedisDatastore(MappingContext mappingContext, Map<String, String> connectionDetails) {
        super(mappingContext, connectionDetails);

        if(connectionDetails != null) {
            host = read(String.class, CONFIG_HOST, connectionDetails, DEFAULT_HOST);
            port = read(Integer.class, CONFIG_PORT, connectionDetails, DEFAULT_PORT);
            timeout = read(Integer.class, CONFIG_TIMEOUT, connectionDetails, 2000);
            pooled = read(Boolean.class, CONFIG_POOLED, connectionDetails, false);
            password = read(String.class, CONFIG_PASSWORD, connectionDetails, null);
            int resourceCount = read(Integer.class, CONFIG_RESOURCE_COUNT, connectionDetails, 10);

            if(pooled && usJedis()) {
                JedisTemplateFactory.createPool(host, port, timeout, resourceCount);
            }
        }

        initializeConverters(mappingContext);
    }

    private void initializeConverters(MappingContext mappingContext) {
        final GenericConversionService conversionService = mappingContext.getConversionService();

        conversionService.addConverter(new Converter<Date, String>() {
            public String convert(Date date) {
                return String.valueOf(date.getTime());
            }
        });

        conversionService.addConverter(new Converter<String, Date>() {

            public Date convert(String s) {
                try {
                    final Long time = Long.valueOf(s);
                    return new Date(time);
                } catch (NumberFormatException e) {
                    // ignore
                }
                return null;
            }
        });


    }

    private boolean usJedis() {
        return jedisClientAvailable && !redisClientAvailable;
    }

    static class JedisTemplateFactory {
        static JedisPool pool;

        static void createPool(String host, int port, int timeout, int resources) {
            pool = new JedisPool(host, port, timeout);
            pool.setResourcesNumber(resources);
        }
        static RedisTemplate create(String host, int port, int timeout, boolean pooled, String password) {

            JedisTemplate template;

            if(pooled) {
                template = new JedisTemplate(pool);
            }
            else {
                template = new JedisTemplate(host, port, timeout);
            }
            if(password != null) {
                template.setPassword(password);
            }

            return template;
        }
    }

    static class RedisClientTemplateFactory {
        static RedisTemplate create(String host, int port, String password) {
            final RedisClientTemplate template = new RedisClientTemplate(host, port);
            if(password != null) {
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
    protected Session createSession(Map<String, String> connectionDetails) {
        if(redisClientAvailable) {
            return new RedisSession(this, getMappingContext(), RedisClientTemplateFactory.create(host, port, password));
        }
        else if(jedisClientAvailable) {
            return new RedisSession(this, getMappingContext(), JedisTemplateFactory.create(host, port, timeout, pooled,password ));
        }
        else {
           throw new IllegalStateException("Cannot create RedisSession. No Redis client library found on classpath. Please use either Jedis or java-redis-client");
        }
    }

    public void afterPropertiesSet() throws Exception {
        if(backgroundIndex) {
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
                    KeyValue kv = (KeyValue) prop.getMapping().getMappedForm();
                    if(kv != null && kv.isIndex()) {
                        indexed.add(prop);
                    }
                }

                if(!indexed.isEmpty()) {
                    // page through entities indexing each one
                    final Class cls = entity.getJavaClass();
                    Query query = session.createQuery(cls);
                    query.projections().count();
                    Long total = (Long) query.singleResult();

                    if(total < 100) {
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
                if(value != null) {
                    RedisEntityPersister persister = (RedisEntityPersister) session.getPersister(entity.getJavaClass());
                    PropertyValueIndexer indexer = persister.getPropertyIndexer(persistentProperty);
                    indexer.index(value, identifier);
                }

            }
        }
    }
}
