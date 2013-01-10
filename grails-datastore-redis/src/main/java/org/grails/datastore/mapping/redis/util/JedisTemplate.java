/* Copyright (C) 2011 SpringSource
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
package org.grails.datastore.mapping.redis.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.NoTransactionException;
import org.springframework.util.ReflectionUtils;

import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.PipelineBlock;
import redis.clients.jedis.Response;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * A Spring-style template for querying Redis and translating
 * Jedis exceptions into Spring exceptions
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public class JedisTemplate implements RedisTemplate<Jedis, SortingParams> {

    public static final String QUEUED = "QUEUED";

    private String password;
    private Jedis redis;
    private Transaction transaction;
    private JedisPool pool;
    private PipelineBlock pipeline;
    private String host = "localhost";
    private int port;
    private int timeout = 2000;

    public JedisTemplate(String host, int port, int timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    public JedisTemplate(Jedis jedis) {
        this.redis = jedis;
    }

    public boolean append(final String key, final Object val) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    Response<?> response = transaction.append(key, val.toString());
                    return QUEUED.equals(response.get());
                }
                if (pipeline != null) {
                    pipeline.append(key, val.toString());
                    return false;
                }
                return redis.append(key, val.toString()) > 0;
            }
        });
    }

    public List<String> blpop(final int timeout, final String... keys) {
        return (List<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.blpop(timeout, keys);
            }
        });
    }

    public List<String> brpop(final int timeout, final String... keys) {
        return (List<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.brpop(timeout, keys);
            }
        });
    }

    public boolean decr(final String key) {
        return (Boolean) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.decr(key) > 0;
            }
        });
    }

    public boolean decrby(final String key, final int amount) {
        return (Boolean) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.decrBy(key, amount) > 0;
            }
        });
    }

    public List<Object> pipeline(final RedisCallback<RedisTemplate<Jedis, SortingParams>> pipeline) {
        return (List<Object>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) throws IOException {
                if (isInMulti()) {
                    // call directly if in a transaction
                    return pipeline.doInRedis(JedisTemplate.this);
                }

                return redis.pipelined(new PipelineBlock() {
                    @Override
                    public void execute() {
                        try {
                            JedisTemplate.this.pipeline = this;
                            pipeline.doInRedis(JedisTemplate.this);
                        } catch (IOException e) {
                            disconnect();
                            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
                        } catch (RuntimeException e) {
                            disconnect();
                            throw e;
                        }
                        finally {
                            JedisTemplate.this.pipeline = null;
                        }
                    }
                });
            }
        });
    }

    private void disconnect() {
        // hack in to get the private client since disconnect() isn't in the interface
        Field field = ReflectionUtils.findField(pipeline.getClass(), "client");
        field.setAccessible(true);
        try {
            Client client = (Client)field.get(pipeline);
            client.disconnect();
        }
        catch (Exception e) {
            ReflectionUtils.handleReflectionException(e);
        }
    }

    public boolean persist(final String redisKey) {
        return (Boolean) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.persist(redisKey) > 0;
            }
        });
    }

    public JedisTemplate(JedisPool pool) {
        this.pool = pool;
    }

    public JedisTemplate(JedisPool pool, int timeout) {
        this.timeout = timeout;
        this.pool = pool;
    }

    public Object execute(RedisCallback<Jedis> jedisRedisCallback) {
        try {
            if (redis == null) {
                redis = getNewConnection();
                doConnect();
            }
            else {
                if (!redis.isConnected()) {
                    try {
                        doConnect();
                    }
                    catch (JedisConnectionException e) {
                        throw new DataAccessResourceFailureException(
                                "Connection failure connecting to Redis: " + e.getMessage(), e);
                    }
                }
            }

            return jedisRedisCallback.doInRedis(redis);
        } catch (IOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    private void doConnect() {
        redis.connect();
        doAuthentication();
    }

    private void doAuthentication() {
        if (password != null) {
            try {
                redis.auth(password);
            } catch (Exception e) {
                throw new DataAccessResourceFailureException("I/O exception authenticating with Redis: " + e.getMessage(), e);
            }
        }
    }

    protected Jedis getNewConnection() {
        Jedis jedis;
        if (pool == null) {
            jedis = new Jedis(host, port, timeout);
        }
        else {
            try {
                jedis = pool.getResource();
            } catch (JedisConnectionException e) {
                throw new DataAccessResourceFailureException("Connection timeout getting Jedis connection from pool: " + e.getMessage(), e);
            }
        }
        return jedis;
    }

    protected void closeNewConnection(Jedis jedis) {
        if (pool == null) {
            jedis.disconnect();
        }
        else {
            pool.returnResource(jedis);
        }
    }

    @SuppressWarnings("rawtypes")
    public SortParams sortParams() {
        return new JedisSortParams();
    }

    public void save() {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    pipeline.save();
                }
                else {
                    redis.save();
                }

                return null;
            }
        });
    }

    public void bgsave() {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    pipeline.save();
                }
                else {
                    redis.bgsave();
                }

                return null;
            }
        });
    }

    public boolean sismember(final String redisKey, final Object o) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    Response<?> response = transaction.sismember(redisKey, o.toString());
                    return QUEUED.equals(response.get());
                }
                if (pipeline != null) {
                    pipeline.sismember(redisKey, o.toString());
                    return false;
                }
                return redis.sismember(redisKey, o.toString());
            }
        });
    }

    public void del(final String redisKey) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.del(redisKey);
                }
                else {
                    if (pipeline != null) {
                        pipeline.del(redisKey);
                    }
                    else {
                        redis.del(redisKey);
                    }
                }
                return null;
            }
        });
    }

    public long scard(final String redisKey) {
        return (Long)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    redis.scard(redisKey);
                    return 0;
                }
                return redis.scard(redisKey);
            }
        });
    }

    public boolean sadd(final String redisKey, final Object o) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    Response<?> response = transaction.sadd(redisKey, o.toString());
                    return QUEUED.equals(response.get());
                }
                if (pipeline != null) {
                    pipeline.sadd(redisKey, o.toString());
                    return false;
                }
                return redis.sadd(redisKey, o.toString()) > 0;
            }
        });
    }

    public boolean srem(final String redisKey, final Object o) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    Response<?> response = transaction.append(redisKey, o.toString());
                    return QUEUED.equals(response.get());
                }
                if (pipeline != null) {
                    pipeline.srem(redisKey, o.toString());
                    return false;
                }
                return redis.srem(redisKey, o.toString()) > 0;
            }
        });
    }

    public Set<String> smembers(final String redisKey) {
        return (Set<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.smembers(redisKey);
            }
        });
    }

    public void lset(final String redisKey, final int index, final Object o) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.lset(redisKey, index, o.toString());
                }
                else {
                    if (pipeline != null) {
                        pipeline.lset(redisKey, index, o.toString());
                    } else {
                        redis.lset(redisKey, index, o.toString());
                    }
                }
                return null;
            }
        });
    }

    public void ltrim(final String redisKey, final int start, final int end) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.ltrim(redisKey, start, end);
                }
                else {
                    if (pipeline != null) {
                        pipeline.ltrim(redisKey, start, end);
                    } else {
                        redis.ltrim(redisKey, start, end);
                    }
                }
                return null;
            }
        });
    }

    public String lindex(final String redisKey, final int index) {
        return (String) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    pipeline.lindex(redisKey, index);
                    return null;
                }
                return redis.lindex(redisKey, index);
            }
        });
    }

    public long llen(final String redisKey) {
        return (Long) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    pipeline.llen(redisKey);
                    return 0;
                }
                return redis.llen(redisKey);
            }
        });
    }

    public List<String> lrange(final String redisKey, final int start, final int end) {
        return (List<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    pipeline.lrange(redisKey, start, end);
                    return null;
                }
                return redis.lrange(redisKey, start, end);
            }
        });
    }

    public String rename(final String old, final String newKey) {
        return (String) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.rename(old, newKey);
                }
                else if (pipeline != null) {
                    pipeline.rename(old, newKey);
                }
                else {
                    return redis.rename(old, newKey);
                }

                return null;
            }
        });

    }

    public String rpop(final String redisKey) {
        return (String) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.rpop(redisKey);
                }
                else if (pipeline != null) {
                    pipeline.rpop(redisKey);
                }
                else {
                    return redis.rpop(redisKey);
                }

                return null;
            }
        });
    }

    public void rpush(final String redisKey, final Object o) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.rpush(redisKey, o.toString());
                }
                else {
                    if (pipeline != null) {
                        pipeline.rpush(redisKey, o.toString());
                    } else {
                        redis.rpush(redisKey, o.toString());
                    }
                }
                return null;
            }
        });
    }

    public long lrem(final String redisKey, final Object o, final int count) {
        return (Long) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.lrem(redisKey, count, o.toString());
                    return 0;
                }
                if (pipeline != null) {
                    pipeline.lrem(redisKey, count, o.toString());
                    return 0;
                }
                return redis.lrem(redisKey, count, o.toString());
            }
        });
    }

    public void flushdb() {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.flushDB();
                return null;
            }
        });
    }

    public void flushall() {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.flushAll();
                return null;
            }
        });
    }

    public void select(final int index) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.select(index);
                return null;
            }
        });
    }

    public long dbsize() {
        return (Long) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.dbSize();
            }
        });
    }

    public void lpush(final String redisKey, final Object o) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.lpush(redisKey, o.toString());
                }
                else {
                    if (pipeline != null) {
                        pipeline.lpush(redisKey, o.toString());
                    } else {
                        redis.lpush(redisKey, o.toString());
                    }
                }

                return null;
            }
        });
    }

    public void lpop(final String redisKey) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.lpop(redisKey);
                }
                else {
                    if (pipeline != null) {
                        pipeline.lpop(redisKey);
                    } else {
                        redis.lpop(redisKey);
                    }
                }

                return null;
            }
        });
    }

    public String hget(final String redisKey, final String entryKey) {
        return (String) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    pipeline.hget(redisKey, entryKey);
                    return null;
                }
                return redis.hget(redisKey, entryKey);
            }
        });
    }

    public long hlen(final String redisKey) {
        return (Long) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    pipeline.hlen(redisKey);
                    return 0;
                }
                return redis.hlen(redisKey);
            }
        });
    }

    public List<String> hkeys(final String redisKey) {
        return (List<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    pipeline.hkeys(redisKey);
                    return null;
                }
                return redis.hkeys(redisKey);
            }
        });
    }

    public boolean hset(final String redisKey, final String key, final Object o) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    return transaction.hset(redisKey, key, o.toString()).equals(QUEUED);
                }
                if (pipeline != null) {
                    pipeline.hset(redisKey, key, o.toString());
                    return false;
                }
                return redis.hset(redisKey, key, o.toString()) > 0;
            }
        });
    }

    public boolean hsetnx(final String redisKey, final String key, final Object o) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    return transaction.hsetnx(redisKey, key, o.toString()).equals(QUEUED);
                }
                if (pipeline != null) {
                    pipeline.hsetnx(redisKey, key, o.toString());
                    return false;
                }
                return redis.hsetnx(redisKey, key, o.toString()) > 0;
            }
        });
    }

    public List<String> hvals(final String redisKey) {
        return (List<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    pipeline.hvals(redisKey);
                    return null;
                }
                return redis.hvals(redisKey);
            }
        });
    }

    public boolean hdel(final String redisKey, final String entryKey) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    return transaction.hdel(redisKey, entryKey).equals(QUEUED);
                }
                if (pipeline != null) {
                    pipeline.hdel(redisKey, entryKey);
                    return false;
                }
                return redis.hdel(redisKey, entryKey) > 0;
            }
        });
    }

    public boolean hexists(final String redisKey, final String entryKey) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    return transaction.hexists(redisKey, entryKey).equals(QUEUED);
                }
                if (pipeline != null) {
                    pipeline.hexists(redisKey, entryKey);
                    return false;
                }
                return redis.hexists(redisKey, entryKey);
            }
        });
    }

    public Map<String, String> hgetall(final String redisKey) {
        return (Map<String, String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    pipeline.hgetAll(redisKey);
                    return null;
                }
                return redis.hgetAll(redisKey);
            }
        });
    }

    public boolean hincrby(final String redisKey, final String entryKey, final int amount) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    return transaction.hincrBy(redisKey, entryKey, amount).equals(QUEUED);
                }
                if (pipeline != null) {
                    pipeline.hincrBy(redisKey, entryKey, amount);
                    return false;
                }
                return redis.hincrBy(redisKey, entryKey, amount) > 0;
            }
        });
    }

    public List<String> hmget(final String hashKey, final String... fields) {
        return (List<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    pipeline.hmget(hashKey, fields);
                    return null;
                }
                return redis.hmget(hashKey, fields);
            }
        });
    }

    public void hmset(final String key, final Map<String, String> nativeEntry) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.hmset(key, nativeEntry);
                }
                else {
                    if (pipeline != null) {
                        pipeline.hmset(key, nativeEntry);
                        return null;
                    }
                    redis.hmset(key, nativeEntry);
                }

                return null;
            }
        });
    }

    public long incr(final String key) {
        return (Long)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) throws IOException {
                if (transaction != null) {
                    redis = getNewConnection();
                    redis.connect();
                    try {
                        return redis.incr(key);
                    }
                    finally {
                        closeNewConnection(redis);
                    }
                }
                return redis.incr(key);
            }
        });
    }

    public long incrby(final String key, final int amount) {
        return (Long)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) throws IOException {
                if (transaction != null) {
                    redis = getNewConnection();
                    redis.connect();
                    try {
                        return redis.incrBy(key, amount);
                    }
                    finally {
                        closeNewConnection(redis);
                    }
                }
                return redis.incrBy(key, amount);
            }
        });
    }

    public long del(final String... redisKey) {
        return (Long)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.del(redisKey);
                    return 0;
                }
                return redis.del(redisKey);
            }
        });
    }

    public Set<String> sinter(final String...keys) {
        return (Set<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.sinter(keys);
            }
        });
    }

    public Set<String> sunion(final String... keys) {
        return (Set<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.sunion(keys);
            }
        });
    }

    public void sinterstore(final String storeKey, final String... keys) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.sinterstore(storeKey, keys);
                }
                else {
                    redis.sinterstore(storeKey, keys);
                }
                return null;
            }
        });
    }

    public void sunionstore(final String storeKey, final String... keys) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.sunionstore(storeKey, keys);
                }
                else {
                    redis.sunionstore(storeKey, keys);
                }
                return null;
            }
        });
    }

    public Set<String> sdiff(final String... keys) {
        return (Set<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.sdiff(keys);
            }
        });
    }

    public boolean smove(final String source, final String destination, final String member) {
        return (Boolean) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.smove(source, destination, member);
                }
                else {
                    if (pipeline != null) {
                        pipeline.smove(source, destination, member);
                        return null;
                    }
                    return redis.smove(source, destination, member) > 0;
                }

                return false;
            }
        });
    }

    public void sdiffstore(final String storeKey, final String... keys) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.sdiffstore(storeKey, keys);
                }
                else {
                    redis.sdiffstore(storeKey, keys);
                }
                return null;
            }
        });
    }

    public boolean setnx(final String redisKey, final Object o) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    return transaction.setnx(redisKey, o.toString()).equals(QUEUED);
                }
                return redis.setnx(redisKey, o.toString()) > 0;
            }
        });
    }

    public long strlen(final String redisKey) {
        return (Long)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.strlen(redisKey);
            }
        });
    }

    public boolean expire(final String key, final int timeout) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    Response<?> response = transaction.expire(key, timeout);
                    return QUEUED.equals(response.get());
                }
                return redis.expire(key,timeout) > 0;
            }
        });
    }

    public long ttl(final String key) {
        return (Long)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.ttl(key);
            }
        });
    }

    public String type(final String key) {
        return (String) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.type(key);
            }
        });
    }

    public String getset(final String redisKey, final Object o) {
        return (String) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.getSet(redisKey, o.toString());
            }
        });
    }

    public Set<String> keys(final String pattern) {
        return (Set<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) throws IOException {
                if (transaction != null) {
                    redis = getNewConnection();
                    redis.connect();
                    try {
                        return redis.keys(pattern);
                    }
                    finally {
                        closeNewConnection(redis);
                    }
                }
                return redis.keys(pattern);
            }
        });
    }

    public void close() {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) throws IOException {
                if (pool != null) {
                    pool.returnResource(redis);
                }
                else {
                    redis.disconnect();
                }
                return null;
            }
        });
    }

    public Object multi() {
        return execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                transaction = redis.multi();
                return transaction;
            }
        });
    }

    public Jedis getRedisClient() {
        return redis;
    }

    public boolean exists(final String key) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.exists(key);
            }
        });
    }

    public String get(final String key) {
        return (String) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.get(key);
            }
        });
    }

    public List<String> mget(final String... keys) {
        return (List<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.mget(keys);
            }
        });
    }

    public void mset(Map<String, String> map) {
        final String[] keysAndValues = new String[map.size()*2];
        int index = 0;
        for (String key : map.keySet()) {
            keysAndValues[index++] = key;
            keysAndValues[index++] = map.get(key);
        }
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.mset(keysAndValues);
                }
                else {
                    redis.mset(keysAndValues);
                }

                return null;
            }
        });
    }

    public Object[] exec() {
        return (Object[]) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    List<Object> results = transaction.exec();
                    try {
                        return results.toArray(new Object[results.size()]);
                    } finally {
                        transaction = null;
                    }
                }
                throw new NoTransactionException("No transaction started. Call multi() first!");
            }
        });
    }

    public void discard() {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) throws IOException {
                if (transaction != null) {
                    transaction.discard();
                    transaction = null;
                    redis.disconnect();
                    JedisTemplate.this.redis = null;
                }

                return null;
            }
        });
    }

    public boolean zadd(final String key, final double rank, final Object o) {
        return (Boolean) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    return transaction.zadd(key, rank, o.toString()).equals(QUEUED);
                }
                if (pipeline != null) {
                    pipeline.zadd(key, rank, o.toString());
                    return true;
                }
                return redis.zadd(key, rank, o.toString()) > 0;
            }
        });
    }

    public long zcount(final String key, final double min, final double max) {
        return (Long) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.zcount(key, min, max);
            }
        });
    }

    public double zincrby(final String key, final double score, final String member) {
        return (Double) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.zincrby(key, score, member);
            }
        });
    }

    public long zinterstore(final String destKey, final String...keys) {
        return (Long) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    pipeline.zinterstore(destKey, keys);
                    return 0;
                }
                return redis.zinterstore(destKey,keys);
            }
        });
    }

    public long zunionstore(final String destKey, final String... keys) {
        return (Long) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    pipeline.zunionstore(destKey, keys);
                    return 0;
                }
                return redis.zunionstore(destKey, keys);
            }
        });
    }

    public long zcard(final String key) {
        return (Long) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.zcard(key);
            }
        });
    }

    public long zrank(final String key, final Object member) {
        return (Long) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    redis.zrank(key, member.toString());
                    return 0;
                }
                return redis.zrank(key, member.toString());
            }
        });
    }

    public long zrem(final String key, final Object member) {
        return (Long) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    pipeline.zrem(key, member.toString());
                    return 0;
                }
                return redis.zrem(key, member.toString());
            }
        });
    }

    public Set<String> zrange(final String key, final int fromIndex, final int toIndex) {
        return (Set<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.zrange(key, fromIndex, toIndex);
            }
        });
    }

    public Set<String> zrangebyscore(final String sortKey, final double rank1, final double rank2) {
        return (Set<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.zrangeByScore(sortKey, rank1, rank2);
            }
        });
    }

    public void set(final String key, final Object value) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.set(key, value.toString());
                }
                else {
                    redis.set(key, value.toString());
                }

                return null;
            }
        });
    }

    public void setex(final String key, final Object value, final int timeout) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (transaction != null) {
                    transaction.setex(key, timeout, String.valueOf(value));
                }
                else {
                    redis.setex(key, timeout, String.valueOf(value));
                }
                return null;
            }
        });
    }

    public Double zscore(final String key, final String member) {
        return (Double) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if (pipeline != null) {
                    pipeline.zscore(key, member);
                    return 0;
                }
                return redis.zscore(key, member);
            }
        });
    }

    public Set<String> zrevrange(final String key, final int start, final int end) {
        return (Set<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.zrevrange(key, start, end);
            }
        });
    }

    public void setPassword(String pass) {
        this.password = pass;
    }

    public String srandmember(final String key) {
        return (String) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.srandmember(key);
            }
        });
    }

    public String spop(final String key) {
        return (String) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.spop(key);
            }
        });
    }

    public List<String> sort(final String key, final SortParams<SortingParams> params) {
        return (List<String>) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.sort(key, params.getParamList().get(0));
            }
        });
    }

    public void sortstore(final String key, final String destKey, final SortParams<SortingParams> params) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.sort(key, params.getParamList().get(0), destKey);
                return null;
            }
        });
    }

    public boolean isInMulti() {
        return getRedisClient().getClient().isInMulti();
    }

    private class JedisSortParams extends SortParams<SortingParams> {
        private SortingParams nativeParams;

        private JedisSortParams() {
            this.nativeParams = new SortingParams();
            getParamList().add(nativeParams);
        }

        @Override
        protected SortingParams createAlpha() {
            nativeParams.alpha();
            return nativeParams;
        }

        @Override
        protected SortingParams createDesc() {
            nativeParams.desc();
            return nativeParams;
        }

        @Override
        protected SortingParams createGet(String pattern) {
            nativeParams.get(pattern);
            return nativeParams;
        }

        @Override
        protected SortingParams createLimit(int start, int count) {
            nativeParams.limit(start, count);
            return nativeParams;
        }

        @Override
        protected SortingParams createAsc() {
            nativeParams.asc();
            return nativeParams;
        }

        @Override
        protected SortingParams createBy(String pattern) {
            nativeParams.by(pattern);
            return nativeParams;
        }
    }
}
