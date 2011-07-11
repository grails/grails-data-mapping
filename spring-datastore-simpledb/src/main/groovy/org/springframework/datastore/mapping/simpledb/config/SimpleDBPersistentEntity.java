package org.springframework.datastore.mapping.simpledb.config;

import org.springframework.datastore.mapping.keyvalue.mapping.config.Family;
import org.springframework.datastore.mapping.model.*;
import org.springframework.datastore.mapping.simpledb.engine.ConstSimpleDBDomainResolver;
import org.springframework.datastore.mapping.simpledb.engine.SimpleDBDomainResolver;

/**
 * Models a SimpleDB-mapped entity
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBPersistentEntity extends AbstractPersistentEntity {

    public SimpleDBPersistentEntity(Class javaClass, MappingContext context) {
        super(javaClass, context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ClassMapping<SimpleDBDomainClassMappedForm> getMapping() {
        return new SimpleDBClassMapping(this, context);
    }

    public class SimpleDBClassMapping extends AbstractClassMapping<SimpleDBDomainClassMappedForm> {
        public SimpleDBClassMapping(PersistentEntity entity, MappingContext context) {
            super(entity, context);
        }
        @Override
        public SimpleDBDomainClassMappedForm getMappedForm() {
            return (SimpleDBDomainClassMappedForm) context.getMappingFactory().createMappedForm(SimpleDBPersistentEntity.this);
        }
    }

}