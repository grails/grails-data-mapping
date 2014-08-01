package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.AbstractClassMapping
import org.grails.datastore.mapping.config.Entity
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity

/**
 * @author Stefan Armbruster
 */
@CompileStatic
class GraphClassMapping extends AbstractClassMapping<Entity> {

    GraphClassMapping(PersistentEntity entity, MappingContext context) {
        super(entity, context)
    }

    @Override
    Entity getMappedForm() {
        ((GraphPersistentEntity)entity).mappedForm
    }
}
