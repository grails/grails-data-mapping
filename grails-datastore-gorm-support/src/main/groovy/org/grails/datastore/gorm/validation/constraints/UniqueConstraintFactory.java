package org.grails.datastore.gorm.validation.constraints;

import grails.validation.Constraint;
import grails.validation.ConstraintFactory;
import org.grails.datastore.mapping.core.Datastore;

/**
 * Factory for the unique constraint.
 *
 *
 * @author Graeme Rocher
 *
 */
public class UniqueConstraintFactory implements ConstraintFactory {

    Datastore datastore;

    public UniqueConstraintFactory(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public Constraint newInstance() {
        return new UniqueConstraint(datastore);
    }
}
