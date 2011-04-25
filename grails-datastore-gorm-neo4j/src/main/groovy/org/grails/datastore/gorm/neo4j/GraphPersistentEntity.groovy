package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.model.AbstractPersistentEntity
import org.springframework.datastore.mapping.model.MappingContext

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 25.04.11
 * Time: 17:30
 * To change this template use File | Settings | File Templates.
 */
class GraphPersistentEntity extends AbstractPersistentEntity {

    public GraphPersistentEntity(Class javaClass, MappingContext context) {
        super(javaClass, context)
    }

}
