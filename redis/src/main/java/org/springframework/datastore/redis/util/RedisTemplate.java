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

import org.jredis.JRedis;
import org.jredis.RedisException;
import org.springframework.dao.DataAccessException;

import java.io.Serializable;
import java.util.List;

/**
 * A Spring-style template for querying Redis and translating
 * JRedis exceptions into Spring exceptions
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisTemplate {

    JRedis jredis;
    
    public RedisTemplate(JRedis jredis) {
        this.jredis = jredis;
    }

    public Object execute(RedisCallback callback) {
        try {
            return callback.doInRedis(jredis);
        } catch (RedisException e) {
            throw new DataAccessException("Exception occured executing Redis command: " + e.getMessage(), e) {};
        }
    }

    public JRedis getJRedis() {
        return jredis;
    }

    public boolean sismember(String redisKey, Object o) {
        try {
            if(o instanceof Number) {
                return jredis.sismember(redisKey, (Number)o);
            }
            else if(o instanceof String) {
                return jredis.sismember(redisKey, (String)o);
            }
            else if(o instanceof byte[]) {
                return jredis.sismember(redisKey, (byte[])o);
            }
            else if(o instanceof Serializable) {
                return jredis.sismember(redisKey, (Serializable)o);
            }
        } catch (RedisException e) {
            throw new DataAccessException("Exception occured executing Redis command [sismember]: " + e.getMessage(), e) {};
        }
        return false;
    }


    public void del(String redisKey) {
       try {
            jredis.del(redisKey);
        } catch (RedisException e) {
            throw new DataAccessException("Exception occured executing Redis command [del]: " + e.getMessage(), e) {};
        }
    }

    public long scard(String redisKey) {
        try {
             return jredis.scard(redisKey);
         } catch (RedisException e) {
             throw new DataAccessException("Exception occured executing Redis command [scard]: " + e.getMessage(), e) {};
         }

    }

    public boolean sadd(String redisKey, Object o) {
        try {
            if(o instanceof Number) {
                return jredis.sadd(redisKey, (Number)o);
            }
            else if(o instanceof String) {
                return jredis.sadd(redisKey, (String)o);
            }
            else if(o instanceof byte[]) {
                return jredis.sadd(redisKey, (byte[])o);
            }
            else if(o instanceof Serializable) {
                return jredis.sadd(redisKey, (Serializable)o);
            }
        } catch (RedisException e) {
            throw new DataAccessException("Exception occured executing Redis command [sadd]: " + e.getMessage(), e) {};
        }
        return false;
    }

    public boolean srem(String redisKey, Object o) {
        try {
            if(o instanceof Number) {
                return jredis.srem(redisKey, (Number)o);
            }
            else if(o instanceof String) {
                return jredis.srem(redisKey, (String)o);
            }
            else if(o instanceof byte[]) {
                return jredis.srem(redisKey, (byte[])o);
            }
            else if(o instanceof Serializable) {
                return jredis.srem(redisKey, (Serializable)o);
            }
        } catch (RedisException e) {
            throw new DataAccessException("Exception occured executing Redis command [srem]: " + e.getMessage(), e) {};
        }
        return false;
    }

    public List<byte[]> smembers(String redisKey) {
        try {
            return jredis.smembers(redisKey);
        } catch (RedisException e) {
            throw new DataAccessException("Exception occured executing Redis command [smembers]: " + e.getMessage(), e) {};
        }
    }

    public void lset(String redisKey, int index, Object o) {
        try {
            if(o instanceof Number) {
                jredis.lset(redisKey,index, (Number)o);
            }
            else if(o instanceof String) {
                jredis.lset(redisKey,index, (String)o);
            }
            else if(o instanceof byte[]) {
                jredis.lset(redisKey,index, (byte[])o);
            }
            else if(o instanceof Serializable) {
                jredis.lset(redisKey, index,(Serializable)o);
            }
        } catch (RedisException e) {
            throw new DataAccessException("Exception occured executing Redis command [lset]: " + e.getMessage(), e) {};
        }
    }

    public byte[] lindex(String redisKey, int index) {
        try {
            return jredis.lindex(redisKey,index);
        } catch (RedisException e) {
            throw new DataAccessException("Exception occured executing Redis command [lindex]: " + e.getMessage(), e) {};
        }
    }

    public long llen(String redisKey) {
        try {
            return jredis.llen(redisKey);
        } catch (RedisException e) {
            throw new DataAccessException("Exception occured executing Redis command [llen]: " + e.getMessage(), e) {};
        }
    }

    public List<byte[]> lrange(String redisKey, long start, long end) {
        try {
            return jredis.lrange(redisKey, start, end);
        } catch (RedisException e) {
            throw new DataAccessException("Exception occured executing Redis command [lrange]: " + e.getMessage(), e) {};
        }
    }

    public void rpush(String redisKey, Object o) {
        try {
            if(o instanceof Number) {
                jredis.rpush(redisKey, (Number)o);
            }
            else if(o instanceof String) {
                jredis.rpush(redisKey,(String)o);
            }
            else if(o instanceof byte[]) {
                jredis.rpush(redisKey,(byte[])o);
            }
            else if(o instanceof Serializable) {
                jredis.rpush(redisKey,(Serializable)o);
            }
        } catch (RedisException e) {
            throw new DataAccessException("Exception occured executing Redis command [rpush]: " + e.getMessage(), e) {};
        }

    }

    public boolean lrem(String redisKey, Object o, int count) {
        try {
             if(o instanceof Number) {
                 return jredis.lrem(redisKey, (Number)o, count) > 0;
             }
             else if(o instanceof String) {
                 return jredis.lrem(redisKey,(String)o, count ) > 0;
             }
             else if(o instanceof byte[]) {
                 return jredis.lrem(redisKey,(byte[])o, count) > 0;
             }
             else if(o instanceof Serializable) {
                 return jredis.lrem(redisKey,(Serializable)o, count) > 0;
             }
         } catch (RedisException e) {
             throw new DataAccessException("Exception occured executing Redis command [rpush]: " + e.getMessage(), e) {};
         }

        return false;
    }

    public void flushdb() {
        try {
            jredis.flushdb();
        } catch (RedisException e) {
            throw new DataAccessException("Exception occured executing Redis command [flushdb]: " + e.getMessage(), e) {};
        }
    }

}
