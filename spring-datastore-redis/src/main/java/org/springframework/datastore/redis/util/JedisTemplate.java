package org.springframework.datastore.redis.util;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.NoTransactionException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Transaction;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * A Spring-style template for querying Redis and translating
 * Jedis exceptions into Spring exceptions
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class JedisTemplate implements RedisTemplate<Jedis, SortingParams> {
    private String password;
    private boolean authenticated;
    private boolean connected;
    private Jedis redis;
    private Transaction transaction;
    private JedisPool pool;
    public static final String QUEUED = "QUEUED";

    public JedisTemplate(String host, int port, int timeout) {
        this.redis = new Jedis(host, port, timeout);
    }

    public JedisTemplate(Jedis jedis) {
        this.redis = jedis;
    }

    public JedisTemplate(JedisPool pool) {
        try {
            this.redis = pool.getResource(2000);
        } catch (TimeoutException e) {
            throw new DataAccessResourceFailureException("Connection timeout geting Jedis connection from pool: " + e.getMessage(), e);        }

        this.pool = pool;
    }

    public JedisTemplate(JedisPool pool, int timeout) {
        try {
            this.redis = pool.getResource(timeout);
        } catch (TimeoutException e) {
            throw new DataAccessResourceFailureException("Connection timeout geting Jedis connection from pool: " + e.getMessage(), e);        }

        this.pool = pool;
    }

    public Object execute(RedisCallback<Jedis> jedisRedisCallback) {
        try {
            if(!connected) {
                redis.connect();
            }
            if(password != null && !authenticated) {
                try {
                    redis.auth(password);
                    authenticated = true;
                } catch (Exception e) {
                       throw new DataAccessResourceFailureException("I/O exception authenticating with Redis: " + e.getMessage(), e);
                }
            }
            return jedisRedisCallback.doInRedis(redis);
        } catch (IOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public SortParams sortParams() {
        return new JedisSortParams();
    }


    public void save() {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.save();
                return null;
            }
        });
    }

    public void bgsave() {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.bgsave();
                return null;
            }
        });
    }

    public boolean sismember(final String redisKey, final Object o) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.sismember(redisKey, o.toString()) > 0;
            }
        });
    }


    public void del(final String redisKey) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.del(redisKey);
                return null;
            }
        });
    }

    public int scard(final String redisKey) {
        return (Integer)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.scard(redisKey);
            }
        });

    }

    public boolean sadd(final String redisKey, final Object o) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if(transaction != null) {
                    String result = transaction.sadd(redisKey, o.toString());
                    return result != null && result.equals(QUEUED);
                }
                else {
                    return redis.sadd(redisKey, o.toString()) > 0;
                }
            }
        });
    }

    public boolean srem(final String redisKey, final Object o) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if(transaction != null) {
                    String result = transaction.srem(redisKey, o.toString());
                    return result != null && result.equals(QUEUED);
                }
                else {                
                    return redis.srem(redisKey, o.toString()) > 0;
                }
            }
        });
    }

    public String[] smembers(final String redisKey) {
        return (String[]) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                Set<String> stringSet = redis.smembers(redisKey);
                return stringSet.toArray(new String[stringSet.size()]);
            }
        });
    }

    public void lset(final String redisKey, final int index, final Object o) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.lset(redisKey, index, o.toString());
                return null;
            }
        });

    }

    public String lindex(final String redisKey, final int index) {
        return (String) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.lindex(redisKey, index);
            }
        });
    }

    public int llen(final String redisKey) {
        return (Integer) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.llen(redisKey);
            }
        });
    }

    public String[] lrange(final String redisKey, final int start, final int end) {
        return (String[]) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                List<String> stringList = redis.lrange(redisKey, start, end);
                return stringList.toArray(new String[stringList.size()]);
            }
        });
    }

    public void rpush(final String redisKey, final Object o) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.rpush(redisKey, o.toString());
                return null;
            }
        });
    }

    public int lrem(final String redisKey, final Object o, final int count) {
        return (Integer) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
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

    public int dbsize() {
        return (Integer) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.dbSize();
            }
        });
    }

    public void lpush(final String redisKey, final Object o) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.lpush(redisKey, o.toString());
                return null;
            }
        });

    }

    public String hget(final String redisKey, final String entryKey) {
        return (String) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.hget(redisKey, entryKey);
            }
        });

    }

    public int hlen(final String redisKey) {
        return (Integer) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.hlen(redisKey);
            }
        });
    }

    public boolean hset(final String redisKey, final String key, final Object o) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.hset(redisKey, key, o.toString()) > 0;
            }
        });
    }

    public boolean hdel(final String redisKey, final String entryKey) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.hdel(redisKey, entryKey) > 0;
            }
        });
    }

    public Map<String, String> hgetall(final String redisKey) {
        final Map map = new HashMap();

        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.hgetAll(redisKey);
            }
        });

        return map;
    }

    public String[] hmget(final String hashKey, final String[] fields) {
        return (String[]) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                List<String> stringList = redis.hmget(hashKey, fields);
                return stringList.toArray(new String[stringList.size()]);
            }
        });

    }

    public void hmset(final String key, final Map<String, String> nativeEntry) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.hmset(key, nativeEntry);
                return null;
            }
        });
    }

    public int incr(final String key) {
        return (Integer)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.incr(key);
            }
        });
    }

    public int del(final String... redisKey) {
        return (Integer)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.del(redisKey);
            }
        });
    }

    public String[] sinter(final String...keys) {
        return (String[]) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                Set<String> stringSet = redis.sinter(keys);
                return stringSet.toArray(new String[stringSet.size()]);
            }
        });
    }

    public String[] sunion(final String... keys) {
        return (String[]) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                Set<String> stringSet = redis.sunion(keys);
                return stringSet.toArray(new String[stringSet.size()]);
            }
        });
    }

    public void sinterstore(final String storeKey, final String... keys) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.sinterstore(storeKey, keys);
                return null;
            }
        });

    }

    public void sunionstore(final String storeKey, final String... keys) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.sunionstore(storeKey, keys);
                return null;
            }
        });

    }

    public boolean setnx(final String redisKey, final Object o) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.setnx(redisKey, o.toString()) > 0;
            }
        });

    }

    public boolean expire(final String key, final int timeout) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if(transaction != null) {
                    String result = transaction.expire(key,timeout);
                    return result != null && result.equals(QUEUED);
                }
                else {
                    return redis.expire(key,timeout) > 0;
                }
            }
        });
    }

    public int ttl(final String key) {
        return (Integer)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.ttl(key);
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

    public String[] keys(final String pattern) {
        return (String[]) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                List<String> stringList = redis.keys(pattern);
                return stringList.toArray(new String[stringList.size()]);
            }
        });
    }

    public void close() {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) throws IOException {
                if(pool != null) {
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
            return this.redis;
    }

    public boolean exists(final String key) {
        return (Boolean)execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.exists(key) > 0;
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

    public void mset(Map<String, String> map) {
        final String[] keysAndValues = new String[map.size()*2];
        int index = 0;
        for (String key : map.keySet()) {
            keysAndValues[index++] = key;
            keysAndValues[index++] = map.get(key);
        }
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.mset(keysAndValues);
                return null;
            }
        });
    }

    public Object[] exec() {
        return (Object[]) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if(transaction != null) {
                    List<Object> results = transaction.exec();
                    try {
                        return results.toArray(new Object[results.size()]);
                    } finally {
                        transaction = null;
                    }
                }
                else {
                    throw new NoTransactionException("No transaction started. Call multi() first!");
                }
            }
        });
    }

    public void discard() {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                if(transaction != null) {
                    transaction.discard();
                    transaction = null;
                }

                return null;
            }
        });
    }

    public boolean zadd(final String key, final double rank, final Object o) {
        return (Boolean) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.zadd(key, rank, o.toString()) > 0;
            }
        });
    }

    public int zrank(final String key, final Object member) {
        return (Integer) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.zrank(key, member.toString());
            }
        });

    }

    public String[] zrange(final String key, final int fromIndex, final int toIndex) {
        return (String[]) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                Set<String> stringSet = redis.zrange(key, fromIndex, toIndex);
                return stringSet.toArray(new String[stringSet.size()]);
            }
        });
    }

    public String[] zrangebyscore(final String sortKey, final double rank1, final double rank2) {
        return (String[]) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                Set<String> stringSet = redis.zrangeByScore(sortKey, rank1, rank2);
                return stringSet.toArray(new String[stringSet.size()]);
            }
        });

    }

    public void set(final String key, final Object value) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.set(key, value.toString());
                return null;
            }
        });
    }

    public void setex(final String key, final Object value, final int timeout) {
        execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                redis.setex(key, timeout, String.valueOf(value));
                return null;
            }
        });
    }

    public Double zscore(final String key, final String member) {
        return (Double) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                return redis.zscore(key, member);
            }
        });
    }

    public String[] zrevrange(final String key, final int start, final int end) {
        return (String[]) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                Set<String> stringSet = redis.zrevrange(key, start, end);
                return stringSet.toArray(new String[stringSet.size()]);
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

    public String[] sort(final String key, final SortParams<SortingParams> params) {
      return (String[]) execute(new RedisCallback<Jedis>() {
            public Object doInRedis(Jedis redis) {
                List<String> stringList = redis.sort(key, params.getParamList().get(0));
                return stringList.toArray(new String[stringList.size()]);
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
