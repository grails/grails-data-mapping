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
package org.grails.datastore.mapping.redis;

import org.grails.datastore.mapping.redis.util.RedisTemplate;
import org.grails.datastore.mapping.transactions.Transaction;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;

/**
 * Represents a Redis transaction
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class RedisTransaction implements Transaction<RedisTemplate> {
    private RedisTemplate redisTemplate;
    private boolean rollbackCalled;
    private boolean commitCalled;

    public RedisTransaction(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void commit() {
        if (rollbackCalled) {
            throw new IllegalTransactionStateException("Cannot call commit after rollback. Start another transaction first!");
        }
        try {
            /*final Object[] objects =*/ redisTemplate.exec();
            commitCalled = true;
        } catch (Exception e) {
            throw new TransactionSystemException("Exception occurred committing back Redis transaction: " + e.getMessage());
        }
    }

    public boolean isActive() {
        return !commitCalled && !rollbackCalled;
    }

    public void setTimeout(int timeout) {
        throw new UnsupportedOperationException("Transaction timeouts not supported in Redis");
    }

    public void rollback() {
        if (rollbackCalled) {
            throw new UnexpectedRollbackException("Cannot rollback Redis transaction. Transaction already rolled back!");
        }
        try {
            redisTemplate.discard();
            rollbackCalled = true;
        } catch (Exception e) {
            throw new TransactionSystemException("Exception occurred rolling back Redis transaction: " + e.getMessage());
        }
    }

    public RedisTemplate getNativeTransaction() {
        return redisTemplate;
    }
}
