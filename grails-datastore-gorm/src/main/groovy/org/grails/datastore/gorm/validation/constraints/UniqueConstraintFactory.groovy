package org.grails.datastore.gorm.validation.constraints

import org.codehaus.groovy.grails.validation.ConstraintFactory
import org.codehaus.groovy.grails.validation.Constraint
import org.grails.datastore.mapping.core.Datastore

/**
 * Factory for the unique constraint
 */
class UniqueConstraintFactory implements ConstraintFactory{

    Datastore datastore

    UniqueConstraintFactory(Datastore datastore) {
        this.datastore = datastore
    }

    @Override
    Constraint newInstance() {
        return new UniqueConstraint(datastore)
    }
}
