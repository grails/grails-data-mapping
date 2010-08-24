package org.springframework.datastore.redis.util;

import sma.RedisClient;

import java.util.Map;

/**
 * Interface for RedisTemplate implementations to implement
 *
 * @param <T> The concrete Redis client class
 */
public interface RedisTemplate<T, S> {
    Object execute(RedisCallback<T> callback);

    SortParams sortParams();

    void save();

    void bgsave();

    boolean sismember(String redisKey, Object o);

    void del(String redisKey);

    int scard(String redisKey);

    boolean sadd(String redisKey, Object o);

    boolean srem(String redisKey, Object o);

    String[] smembers(String redisKey);

    void lset(String redisKey, int index, Object o);

    String lindex(String redisKey, int index);

    int llen(String redisKey);

    String[] lrange(String redisKey, int start, int end);

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

    String[] hmget(String hashKey, String[] fields);

    void hmset(String key, Map<String, String> nativeEntry);

    int incr(String key);

    int del(String... redisKey);

    String[] sinter(String...keys);

    String[] sunion(String... keys);

    void sinterstore(String storeKey, String... keys);

    void sunionstore(String storeKey, String... keys);

    boolean setnx(String redisKey, Object o);

    boolean expire(String key, int timeout);

    int ttl(String key);

    String getset(String redisKey, Object o);

    String[] keys(String pattern);

    void close();

    Object multi();

    boolean exists(String key);

    String get(String key);

    void mset(Map<String, String> map);

    Object[] exec();

    void discard();

    boolean zadd(String key, double rank, Object o);

    int zrank(String key, Object member);

    String[] zrange(String key, int fromIndex, int toIndex);

    String[] zrangebyscore(String sortKey, double rank1, double rank2);

    void set(String key, Object value);

    void setex(String key, Object value, int timeout);

    Double zscore(String key, String member);

    String[] zrevrange(String key, int start, int end);

    void setPassword(String pass);

    String srandmember(String key);

    String spop(String key);

    public String[] sort(final String key, final SortParams<S> params);

    public void sortstore(final String key, final String destKey,  final SortParams<S> params);

    T getRedisClient();
}
