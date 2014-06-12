package org.grails.datastore.gorm.redis.plugin.support

import org.grails.datastore.gorm.plugin.support.OnChangeHandler
import org.grails.datastore.mapping.core.Datastore
import org.springframework.transaction.PlatformTransactionManager

/**
 * onChange handler for Redis
 */
class RedisOnChangeHandler extends OnChangeHandler {

    RedisOnChangeHandler(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    @Override
    String getDatastoreType() { "Redis" }
}
