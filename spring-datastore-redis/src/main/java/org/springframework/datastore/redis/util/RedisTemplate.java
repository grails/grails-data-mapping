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


import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for RedisTemplate implementations to implement
 *
 * @param <T> The concrete Redis client class
 */
public interface RedisTemplate<T, S> {
    
    List<Object> pipeline(RedisCallback<RedisTemplate<T,S>> pipeline);

    Object execute(RedisCallback<T> callback);

    SortParams sortParams();

    void save();

    void bgsave();

    boolean sismember(String redisKey, Object o);

    void del(String redisKey);

    int scard(String redisKey);

    boolean sadd(String redisKey, Object o);

    boolean srem(String redisKey, Object o);

    Set<String> smembers(String redisKey);

    void lset(String redisKey, int index, Object o);

    String lindex(String redisKey, int index);

    int llen(String redisKey);

    List<String> lrange(String redisKey, int start, int end);

    void rpush(String redisKey, Object o);

    int lrem(String redisKey, Object o, int count);

    void flushdb();

    void flushall();

    void select(int index);

    int dbsize();

    void lpush(String redisKey, Object o);

    String hget(String redisKey, String entryKey);

    int hlen(String redisKey);

    boolean hset(String redisKey, String key, Object o);

    boolean hdel(String redisKey, String entryKey);

    Map<String, String> hgetall(String redisKey);

    List<String> hmget(String hashKey, String... fields);

    void hmset(String key, Map<String, String> nativeEntry);

    int incr(String key);

    int del(String... redisKey);

    Set<String> sinter(String...keys);

    Set<String> sunion(String... keys);

    void sinterstore(String storeKey, String... keys);

    void sunionstore(String storeKey, String... keys);

    void sdiffstore(String key, String... otherKeys);

    boolean setnx(String redisKey, Object o);

    boolean expire(String key, int timeout);

    int ttl(String key);

    String getset(String redisKey, Object o);

    List<String> keys(String pattern);

    void close();

    Object multi();

    boolean exists(String key);

    String get(String key);

    void mset(Map<String, String> map);

    Object[] exec();

    void discard();

    boolean zadd(String key, double rank, Object o);

    int zrank(String key, Object member);

    Set<String> zrange(String key, int fromIndex, int toIndex);

    Set<String> zrangebyscore(String sortKey, double rank1, double rank2);

    void set(String key, Object value);

    void setex(String key, Object value, int timeout);

    Double zscore(String key, String member);

    Set<String> zrevrange(String key, int start, int end);

    void setPassword(String pass);

    String srandmember(String key);

    String spop(String key);

    public List<String> sort(final String key, final SortParams<S> params);

    public void sortstore(final String key, final String destKey,  final SortParams<S> params);

    T getRedisClient();


}
