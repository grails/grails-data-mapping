package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.config.AbstractGormMappingFactory
import org.springframework.datastore.mapping.config.Property
import org.springframework.datastore.mapping.keyvalue.mapping.config.Family

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 25.04.11
 * Time: 17:39
 * To change this template use File | Settings | File Templates.
 */
class GraphGormMappingFactory extends AbstractGormMappingFactory {

    @Override
    protected Class getPropertyMappedFormType() {
        Property.class
    }

    @Override
    protected Class getEntityMappedFormType() {
        Family.class
    }

}
