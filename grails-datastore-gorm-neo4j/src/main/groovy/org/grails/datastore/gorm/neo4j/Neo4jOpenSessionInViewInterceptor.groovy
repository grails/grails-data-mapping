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
package org.grails.datastore.gorm.neo4j

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.transactions.Transaction
import org.grails.datastore.mapping.web.support.OpenSessionInViewInterceptor
import org.springframework.web.context.request.WebRequest

/**
 * provide a transaction context for each request
 * since the interceptor might be called multiple times for the same request (by e.g. using <g:include>)
 * all previous transactions must be kept for releasing them later. For this a Stack is used.
 */
class Neo4jOpenSessionInViewInterceptor extends OpenSessionInViewInterceptor {

    protected final Logger log = LoggerFactory.getLogger(getClass())

    Stack<Transaction> transactions = []

    @Override
    void preHandle(WebRequest request) {
        if (log.debugEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
            log.debug "preHandle ${request.getDescription(true)}"
        }
        super.preHandle(request)
        transactions.push(DatastoreUtils.getSession(datastore, true).beginTransaction())
    }

    @Override
    void afterCompletion(WebRequest request, Exception ex) {
        super.afterCompletion(request, ex)
        Transaction transaction = transactions.pop()
        ex ? transaction.rollback() : transaction.commit()
        if (log.debugEnabled) { // TODO: add @Slf4j annotation when groovy 1.8 is used
            log.debug "afterCompletion ${request.getDescription(true)}"
        }
    }
}
