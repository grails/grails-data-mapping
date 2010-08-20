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
package org.springframework.datastore.redis.util;

import org.springframework.dao.DataAccessResourceFailureException;
import sma.RedisClient;

import java.util.HashMap;
import java.util.Map;

/**
 * A Spring-style template for querying Redis and translating
 * JRedis exceptions into Spring exceptions
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisTemplate {

    RedisClient redis;
    private String password;
    private boolean authenticated = false;

    public RedisTemplate(RedisClient jredis) {
        this.redis = jredis;
    }

    public Object execute(RedisCallback callback) {
        try {
            if(password != null && !authenticated) {
                try {
                    redis.auth(password);
                    authenticated = true;
                } catch (RedisClient.RuntimeIOException e) {
                       throw new DataAccessResourceFailureException("I/O exception authenticating with Redis: " + e.getMessage(), e);
                }
            }
            return callback.doInRedis(redis);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }


    public void save() {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.save();
                return null;
            }
        });
    }

    public void bgsave() {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.bgsave();
                return null;
            }
        });
    }

    public boolean sismember(final String redisKey, final Object o) {
        return (Boolean)execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.sismember(redisKey, o.toString());
            }
        });
    }


    public void del(final String redisKey) {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.del(redisKey);
                return null;
            }
        });
    }

    public int scard(final String redisKey) {
        return (Integer)execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.scard(redisKey);
            }
        });

    }

    public boolean sadd(final String redisKey, final Object o) {
        return (Boolean)execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.sadd(redisKey, o.toString());
            }
        });
    }

    public boolean srem(final String redisKey, final Object o) {
        return (Boolean)execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.srem(redisKey, o.toString());
            }
        });
    }

    public String[] smembers(final String redisKey) {
        return (String[]) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                    return redis.smembers(redisKey);
            }
        });
    }

    public void lset(final String redisKey, final int index, final Object o) {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.lset(redisKey, index, o.toString());
                return null;
            }
        });

    }

    public String lindex(final String redisKey, final int index) {
        return (String) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.lindex(redisKey, index);
            }
        });
    }

    public long llen(final String redisKey) {
        return (Long) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.llen(redisKey);
            }
        });
    }

    public String[] lrange(final String redisKey, final int start, final int end) {
        return (String[]) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.lrange(redisKey, start, end);
            }
        });
    }

    public void rpush(final String redisKey, final Object o) {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.rpush(redisKey, o.toString());
                return null;
            }
        });
    }

    public int lrem(final String redisKey, final Object o, final int count) {
        return (Integer) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.lrem(redisKey, count, o.toString());
            }
        });
    }

    public void flushdb() {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.flushdb();
                return null;
            }
        });

    }

    public void flushall() {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.flushall();
                return null;
            }
        });

    }

    public void select(final int index) {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.selectdb(index);
                return null;
            }
        });

    }

    public int dbsize() {
        return (Integer) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.dbsize();
            }
        });
    }

    public void lpush(final String redisKey, final Object o) {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.lpush(redisKey, o.toString());
                return null;
            }
        });

    }

    public String hget(final String redisKey, final String entryKey) {
        return (String) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.hget(redisKey, entryKey);
            }
        });

    }

    public long hlen(final String redisKey) {
        return (Long) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.hlen(redisKey);
            }
        });
    }

    public boolean hset(final String redisKey, final String key, final Object o) {
        return (Boolean)execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.hset(redisKey, key, o.toString());
            }
        });
    }

    public boolean hdel(final String redisKey, final String entryKey) {
        return (Boolean)execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.hdel(redisKey, entryKey);
            }
        });
    }

    public Map<String, String> hgetall(final String redisKey) {
        final Map map = new HashMap();

        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {

                String[] result;
                try {
                    result = redis.hgetall(redisKey);
                } catch (RedisClient.RuntimeIOException e) {
                    throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
                }

                if(result.length>1) {

                    for (int i = 0; i < result.length; i = i + 2) {
                        map.put(result[i], result[i+1]);

                    }
                }
                return null;
            }
        });

        return map;
    }

    public String[] hmget(final String hashKey, final String[] fields) {
        return (String[]) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.hmget(hashKey, fields);
            }
        });

    }

    public void hmset(final String key, final Map<String, String> nativeEntry) {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.hmset(key, nativeEntry);
                return null;
            }
        });
    }

    public long incr(final String key) {
        return (Long)execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.incr(key);
            }
        });
    }

    public long del(final String... redisKey) {
        return (Long)execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.del(redisKey);
            }
        });
    }

    public String[] sinter(final String...keys) {
        return (String[]) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.sinter(keys);
            }
        });
    }

    public String[] sunion(final String... keys) {
        return (String[]) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.sunion(keys);
            }
        });
    }

    public void sinterstore(final String storeKey, final String... keys) {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.sinterstore(storeKey, keys);
                return null;
            }
        });

    }

    public void sunionstore(final String storeKey, final String... keys) {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.sunionstore(storeKey, keys);
                return null;
            }
        });

    }

    public boolean setnx(final String redisKey, final Object o) {
        return (Boolean)execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.setnx(redisKey, o.toString());
            }
        });

    }

    public boolean expire(final String key, final int timeout) {
        return (Boolean)execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.expire(key,timeout);
            }
        });
    }

    public long ttl(final String key) {
        return (Long)execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.ttl(key);
            }
        });
    }

    public String getset(final String redisKey, final Object o) {
        return (String) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.getset(redisKey, o.toString());
            }
        });

    }

    public String[] keys(final String pattern) {
        return (String[]) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.keys(pattern);
            }
        });
    }

    public void close() {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.close();
                return null;
            }
        });

    }

    public void multi() {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.multi();
                return null;
            }
        });

    }

    public RedisClient getRedisClient() {
            return this.redis;
    }

    public boolean exists(final String key) {
        return (Boolean)execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.exists(key);
            }
        });

    }

    public String get(final String key) {
        return (String) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
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
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.mset(keysAndValues);
                return null;
            }
        });
    }

    public Object[] exec() {
        return (Object[]) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.exec();
            }
        });
    }

    public void discard() {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.discard();
                return null;
            }
        });
    }

    public boolean zadd(final String key, final double rank, final Object o) {
        return (Boolean) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.zadd(key, rank, o.toString());
            }
        });
    }

    public int zrank(final String key, final Object member) {
        return (Integer) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.zrank(key, member.toString());
            }
        });

    }

    public String[] zrange(final String key, final int fromIndex, final int toIndex) {
        return (String[]) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.zrange(key, fromIndex, toIndex);
            }
        });
    }

    public String[] zrangebyscore(final String sortKey, final double rank1, final double rank2) {
        return (String[]) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.zrangebyscore(sortKey, rank1, rank2);
            }
        });

    }

    public void set(final String key, final Object value) {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.set(key, value.toString());
                return null;
            }
        });
    }

    public void setex(final String key, final Object value, final int timeout) {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.setex(key, String.valueOf(value), timeout);
                return null;
            }
        });
    }

    public Double zscore(final String key, final String member) {
        return (Double) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.zscore(key, member);
            }
        });
    }

    public String[] zrevrange(final String key, final int start, final int end) {
        return (String[]) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.zrevrange(key, start, end);
            }
        });

    }

    public String[] sort(final String key, final RedisClient.SortParam... params) {
        return (String[]) execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                return redis.sort(key, params);
            }
        });
    }

    public void sortstore(final String key, final String destKey, final RedisClient.SortParam... params) {
        execute(new RedisCallback() {
            public Object doInRedis(RedisClient redis) {
                redis.sortstore(key, destKey, params);
                return null;
            }
        });

    }

    public void setPassword(String pass) {
        this.password = pass;
    }
}
