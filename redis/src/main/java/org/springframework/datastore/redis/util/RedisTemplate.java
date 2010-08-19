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
    
    public RedisTemplate(RedisClient jredis) {
        this.redis = jredis;
    }

    public Object execute(RedisCallback callback) {
        try {
            return callback.doInRedis(redis);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }


    public void save() {
        try {
            redis.save();
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public void bgsave() {
        try {
            redis.bgsave();
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public boolean sismember(String redisKey, Object o) {
        try {
            return redis.sismember(redisKey, o.toString());
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }


    public void del(String redisKey) {
        try {
            redis.del(redisKey);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public int scard(String redisKey) {
        try {
            return redis.scard(redisKey);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public boolean sadd(String redisKey, Object o) {
        try {
            return redis.sadd(redisKey, o.toString());
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public boolean srem(String redisKey, Object o) {
        try {
            return redis.srem(redisKey, o.toString());
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public String[] smembers(String redisKey) {
        try {
            return redis.smembers(redisKey);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public void lset(String redisKey, int index, Object o) {
        try {
            redis.lset(redisKey, index, o.toString());
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public String lindex(String redisKey, int index) {
        try {
            return redis.lindex(redisKey, index);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public long llen(String redisKey) {
        try {
            return redis.llen(redisKey);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public String[] lrange(String redisKey, int start, int end) {
        try {
            return redis.lrange(redisKey, start, end);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public void rpush(String redisKey, Object o) {
        try {
            redis.rpush(redisKey, o.toString());
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public int lrem(String redisKey, Object o, int count) {
        try {
            return redis.lrem(redisKey, count, o.toString());
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public void flushdb() {
        try {
            redis.flushdb();
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public void flushall() {
        try {
            redis.flushall();
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public void select(int index) {
        try {
            redis.selectdb(index);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public int dbsize() {
        try {
            return redis.dbsize();
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public void lpush(String redisKey, Object o) {
        try {
            redis.lpush(redisKey, o.toString());
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public String hget(String redisKey, String entryKey) {
        try {
            return redis.hget(redisKey, entryKey);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public long hlen(String redisKey) {
        try {
            return redis.hlen(redisKey);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public boolean hset(String redisKey, String key, Object o) {
        try {
            return redis.hset(redisKey, key, o.toString());
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public boolean hdel(String redisKey, String entryKey) {
        try {
            return redis.hdel(redisKey, entryKey);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public Map<String, String> hgetall(String redisKey) {
        String[] result;
        try {
            result = redis.hgetall(redisKey);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

        Map map = new HashMap();
        if(result.length>1) {

            for (int i = 0; i < result.length; i = i + 2) {
                map.put(result[i], result[i+1]);

            }
        }
        return map;
    }

    public String[] hmget(String hashKey, String[] fields) {
        try {
            return redis.hmget(hashKey, fields);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public void hmset(String key, Map<String, String> nativeEntry) {
        try {
            redis.hmset(key, nativeEntry);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public long incr(String key) {
        try {
            return redis.incr(key);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public long del(String... redisKey) {
        try {
            return redis.del(redisKey);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public String[] sinter(String...keys) {
        try {
            return redis.sinter(keys);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public String[] sunion(String... keys) {
        try {
            return redis.sunion(keys);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public void sinterstore(String storeKey, String... keys) {
        try {
            redis.sinterstore(storeKey, keys);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public void sunionstore(String storeKey, String... keys) {
        try {
            redis.sunionstore(storeKey, keys);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public boolean setnx(String redisKey, Object o) {
        try {
            return redis.setnx(redisKey, o.toString());
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public boolean expire(String key, int timeout) {
        try {
            return redis.expire(key,timeout);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public long ttl(String key) {
        try {
            return redis.ttl(key);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public String getset(String redisKey, Object o) {
        try {
            return redis.getset(redisKey, o.toString());
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public String[] keys(String pattern) {
        try {
            return redis.keys(pattern);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public void close() {
        try {
            redis.close();
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public void multi() {
        try {
            redis.multi();
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public RedisClient getRedisClient() {
            return this.redis;
    }

    public boolean exists(String key) {
        try {
            return redis.exists(key);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public String get(String key) {
        try {
            return redis.get(key);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public void mset(Map<String, String> map) {
        String[] keysAndValues = new String[map.size()*2];
        int index = 0;
        for (String key : map.keySet()) {
            keysAndValues[index++] = key;
            keysAndValues[index++] = map.get(key);
        }
        try {
            redis.mset(keysAndValues);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public Object[] exec() {
        try {
            return redis.exec();
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public void discard() {
        try {
            redis.discard();
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public boolean zadd(String key, double rank, Object o) {
        try {
            return redis.zadd(key, rank, o.toString());
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public int zrank(String key, Object member) {
        try {
            return redis.zrank(key, member.toString());
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public String[] zrange(String key, int fromIndex, int toIndex) {
        try {
            return redis.zrange(key, fromIndex, toIndex);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public String[] zrangebyscore(String sortKey, double rank1, double rank2) {
        try {
            return redis.zrangebyscore(sortKey, rank1, rank2);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public void set(String key, Object value) {
        try {
            redis.set(key, value.toString());
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public void setex(String key, Object value, int timeout) {
        try {
            redis.setex(key, String.valueOf(value), timeout);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public Double zscore(String key, String member) {
        try {
            return redis.zscore(key, member);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }

    public String[] zrevrange(String key, int start, int end) {
        try {
            return redis.zrevrange(key, start, end);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }

    }

    public String[] sort(String key, RedisClient.SortParam... params) {
        try {
            return redis.sort(key, params);
        } catch (RedisClient.RuntimeIOException e) {
            throw new DataAccessResourceFailureException("I/O exception thrown connecting to Redis: " + e.getMessage(), e);
        }
    }
}
