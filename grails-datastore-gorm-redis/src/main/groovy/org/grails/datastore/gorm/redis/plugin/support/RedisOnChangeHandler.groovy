package org.grails.datastore.gorm.redis.plugin.support

import org.grails.datastore.gorm.plugin.support.OnChangeHandler

/**
 * onChange handler for Redis
 */
class RedisOnChangeHandler extends OnChangeHandler {
    @Override
    String getDatastoreType() { "Redis" }
}
