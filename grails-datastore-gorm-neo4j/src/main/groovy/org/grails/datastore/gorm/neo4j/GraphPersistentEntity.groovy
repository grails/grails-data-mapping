package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.config.Entity
import org.grails.datastore.mapping.model.AbstractPersistentEntity
import org.grails.datastore.mapping.model.MappingContext

@CompileStatic
class GraphPersistentEntity extends AbstractPersistentEntity<Entity> {

    GraphPersistentEntity(Class javaClass, MappingContext context) {
        super(javaClass, context)
    }

    /**
     * recursively join all discriminators up the class hierarchy
     * @return
     */
    public String getLabelsWithInheritance() {
        StringBuilder sb = new StringBuilder()
        appendRecursive(sb)
        sb.toString()
    }

    private void appendRecursive(StringBuilder sb){
        sb.append(":").append(discriminator)
        if (parentEntity!=null) {
            ((GraphPersistentEntity)parentEntity).appendRecursive(sb)
        }
    }
}
