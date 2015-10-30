package org.grails.datastore.gorm.validation.constraints;


import org.codehaus.groovy.grails.validation.ConstraintFactory;
import org.codehaus.groovy.grails.validation.Constraint;
import org.grails.datastore.mapping.core.Datastore;

/**
 * Factory for the unique constraint.
 *
 * Note: Uses the deprecated Grails 2.x APIs to maintain compatibility, change to 3.x APIs in the future
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
