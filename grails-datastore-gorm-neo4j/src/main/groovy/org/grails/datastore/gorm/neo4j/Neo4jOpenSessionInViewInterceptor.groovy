package org.grails.datastore.gorm.neo4j

import org.springframework.web.context.request.WebRequest
import org.springframework.ui.ModelMap
import org.springframework.datastore.mapping.web.support.OpenSessionInViewInterceptor
import org.springframework.datastore.mapping.core.DatastoreUtils;

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 08.05.11
 * Time: 23:15
 * To change this template use File | Settings | File Templates.
 */
class Neo4jOpenSessionInViewInterceptor extends OpenSessionInViewInterceptor {

    def transaction

    @Override
    void preHandle(WebRequest request) {
        super.preHandle(request)
        def session = DatastoreUtils.getSession(datastore, true)
        transaction = session.beginTransaction()
    }

    @Override
    void afterCompletion(WebRequest request, Exception ex) {
        super.afterCompletion(request, ex)
        ex ? transaction.rollback() : transaction.commit()
    }

    /*@Override
    void postHandle(WebRequest request, ModelMap model) {
        super.postHandle(request, model)    //To change body of overridden methods use File | Settings | File Templates.
    } */


}
