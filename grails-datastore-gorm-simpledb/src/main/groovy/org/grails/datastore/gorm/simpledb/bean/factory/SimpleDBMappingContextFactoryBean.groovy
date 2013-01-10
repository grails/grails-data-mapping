package org.grails.datastore.gorm.simpledb.bean.factory

import org.grails.datastore.gorm.bean.factory.AbstractMappingContextFactoryBean
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.simpledb.config.SimpleDBMappingContext

/**
 * Factory bean for construction the SimpleDB MappingContext.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
class SimpleDBMappingContextFactoryBean extends AbstractMappingContextFactoryBean {
    @Override
    protected MappingContext createMappingContext() {
        return new SimpleDBMappingContext()
    }
}