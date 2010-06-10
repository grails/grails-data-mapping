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
package org.grails.inconsequential.redis;

import org.grails.inconsequential.tx.Transaction;
import org.jredis.JRedis;
import org.jredis.RedisException;
import org.springframework.transaction.TransactionSystemException;

/**
 * Represents a Redis transaction
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisTransaction implements Transaction<JRedis> {
    private JRedis jredis;

    public RedisTransaction(JRedis jredis) {
        this.jredis = jredis;
    }

    public void commit() {
        try {
            jredis.exec();
        } catch (RedisException e) {
            throw new TransactionSystemException("Cannot exec Redis transaction: " + e.getMessage(),e);
        }
    }

    public void rollback() {
        try {
            jredis.discard();
        } catch (RedisException e) {
            throw new TransactionSystemException("Cannot rollback Redis transaction: " + e.getMessage(),e);
        }
    }

    public org.jredis.JRedis getNativeTransaction() {
        return jredis;
    }
}
