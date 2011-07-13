package org.grails.datastore.gorm.neo4j

import org.springframework.web.context.request.WebRequest
import org.springframework.datastore.mapping.web.support.OpenSessionInViewInterceptor
import org.springframework.datastore.mapping.core.DatastoreUtils
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import org.springframework.datastore.mapping.transactions.Transaction

/**
 * provide a transaction context for each request
 * since the interceptor might be called multiple times for the same request (by e.g. using <g:include>)
 * all previous transactions must be kept for releasing them later. For this a Stack is used.
 */
class Neo4jOpenSessionInViewInterceptor extends OpenSessionInViewInterceptor {

    private static final Logger log = LoggerFactory.getLogger(Neo4jOpenSessionInViewInterceptor.class)
    Stack<Transaction> transactions = new Stack()

    @Override
    void preHandle(WebRequest request) {
        log.debug "preHandle ${request.getDescription(true)}"
        super.preHandle(request)
        def session = DatastoreUtils.getSession(datastore, true)
        transactions.push(session.beginTransaction())
    }

    @Override
    void afterCompletion(WebRequest request, Exception ex) {
        super.afterCompletion(request, ex)
        def transaction = transactions.pop()
        ex ? transaction.rollback() : transaction.commit()
        log.debug "afterCompletion ${request.getDescription(true)}"
    }

}
