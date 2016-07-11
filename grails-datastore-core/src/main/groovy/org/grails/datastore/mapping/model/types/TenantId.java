package org.grails.datastore.mapping.model.types;

import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.model.AbstractPersistentProperty;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;

import java.beans.PropertyDescriptor;

/**
 * Represents the mapping of a tenant id for multi tenancy
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public abstract class TenantId<T extends Property> extends AbstractPersistentProperty<T> {
    public TenantId(PersistentEntity owner, MappingContext context, String name, Class type) {
        super(owner, context, name, type);
    }

    public TenantId(PersistentEntity owner, MappingContext context, PropertyDescriptor descriptor) {
        super(owner, context, descriptor);
    }

}
