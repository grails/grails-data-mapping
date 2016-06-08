package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.neo4j.mapping.config.Neo4jEntity
import org.grails.datastore.mapping.model.AbstractClassMapping
import org.grails.datastore.mapping.config.Entity
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity

/**
 * Represents a mapping between a GORM entity and the Graph
 *
 * @author Stefan Armbruster
 * @author Graeme Rocher
 */
@CompileStatic
class GraphClassMapping extends AbstractClassMapping<Neo4jEntity> {

    GraphClassMapping(PersistentEntity entity, MappingContext context) {
        super(entity, context)
    }

    @Override
    Neo4jEntity getMappedForm() {
        ((GraphPersistentEntity)entity).mappedForm
    }
}
