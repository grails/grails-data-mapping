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
package org.springframework.datastore.mapping.redis.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for RedisTemplate implementations to implement
 *
 * @param <T> The concrete Redis client class
 */
public interface RedisTemplate<T, S> {

    /**
     * See http://redis.io/commands/append
     */
    boolean append(String key, Object val);


    /**
     * See http://redis.io/commands/blpop
     */
    List<String> blpop(int timeout, String...keys);

    /**
     * See http://redis.io/commands/brpop
     */
    List<String> brpop(int timeout, String...keys);

    /**
     * See http://redis.io/commands/decr
     */
    boolean decr(String key);

    /**
     * See http://redis.io/commands/decrby
     */
    boolean decrby(String key, int amount);

    /**
     * See http://redis.io/commands/del
     */
    void del(String redisKey);

    /**
     * See http://redis.io/commands/del
     */
    long del(String... redisKey);


    List<Object> pipeline(RedisCallback<RedisTemplate<T,S>> pipeline);

    boolean persist(String redisKey);

    Object execute(RedisCallback<T> callback);

    SortParams sortParams();

    void save();

    void bgsave();

    boolean sismember(String redisKey, Object o);


    long scard(String redisKey);

    boolean sadd(String redisKey, Object o);


    boolean srem(String redisKey, Object o);

    Set<String> smembers(String redisKey);

    void lset(String redisKey, int index, Object o);

    void ltrim(String redisKey, int start, int end);

    String lindex(String redisKey, int index);

    long llen(String redisKey);

    List<String> lrange(String redisKey, int start, int end);

    String rename(String old, String newKey);

    String rpop(String redisKey);

    void rpush(String redisKey, Object o);

    long lrem(String redisKey, Object o, int count);

    void flushdb();

    void flushall();

    void select(int index);

    long dbsize();

    void lpush(String redisKey, Object o);

    void lpop(String redisKey);

    String hget(String redisKey, String entryKey);

    long hlen(String redisKey);

    List<String> hkeys(String redisKey);

    boolean hset(String redisKey, String key, Object o);

    boolean hsetnx(String redisKey, String key, Object o);

    List<String> hvals(String redisKey);

    boolean hdel(String redisKey, String entryKey);

    boolean hexists(String redisKey, String entryKey);

    Map<String, String> hgetall(String redisKey);

    boolean hincrby(String redisKey, String entryKey, int amount);

    List<String> hmget(String hashKey, String... fields);

    void hmset(String key, Map<String, String> nativeEntry);

    long incr(String key);

    long incrby(String key, int amount);


    Set<String> sinter(String...keys);

    Set<String> sunion(String... keys);

    void sinterstore(String storeKey, String... keys);

    void sunionstore(String storeKey, String... keys);

    Set<String> sdiff(final String... keys);

    boolean smove(String source, String destination, String member);

    void sdiffstore(String key, String... otherKeys);

    boolean setnx(String redisKey, Object o);

    long strlen(String redisKey);

    boolean expire(String key, int timeout);

    long ttl(String key);

    String type(String key);

    String getset(String redisKey, Object o);

    Set<String> keys(String pattern);

    void close();

    Object multi();

    boolean exists(String key);

    String get(String key);

    List<String> mget(String...keys);

    void mset(Map<String, String> map);

    Object[] exec();

    void discard();

    boolean zadd(String key, double rank, Object o);

    long zcard(String key);

    long zcount(String key, double min, double max);

    double zincrby(String key, double score, String member);

    long zrank(String key, Object member);

    Set<String> zrange(String key, int fromIndex, int toIndex);

    Set<String> zrangebyscore(String sortKey, double rank1, double rank2);

    void set(String key, Object value);

    void setex(String key, Object value, int timeout);

    Double zscore(String key, String member);

    Set<String> zrevrange(String key, int start, int end);

    void setPassword(String pass);

    String srandmember(String key);

    String spop(String key);

    List<String> sort(String key, SortParams<S> params);

    void sortstore(String key, String destKey,  SortParams<S> params);

    T getRedisClient();
}
