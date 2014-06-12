package org.grails.datastore.gorm.dynamodb.bean.factory

import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.grails.datastore.gorm.bean.factory.AbstractMappingContextFactoryBean
import org.grails.datastore.mapping.dynamodb.config.DynamoDBMappingContext
import org.grails.datastore.mapping.model.MappingContext

/**
 * Factory bean for construction the DynamoDB MappingContext.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
class DynamoDBMappingContextFactoryBean extends AbstractMappingContextFactoryBean {

    GrailsPluginManager pluginManager

    @Override
    protected MappingContext createMappingContext() {
        return new DynamoDBMappingContext()
    }
}