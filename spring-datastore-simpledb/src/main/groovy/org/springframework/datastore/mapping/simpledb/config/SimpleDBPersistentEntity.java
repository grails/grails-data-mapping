package org.springframework.datastore.mapping.simpledb.config;

import org.springframework.datastore.mapping.keyvalue.mapping.config.Family;
import org.springframework.datastore.mapping.keyvalue.mapping.config.KeyValuePersistentEntity;
import org.springframework.datastore.mapping.model.*;

/**
 * Models a SimpleDB-mapped entity
 *
 * @author Roman Stepanenko
 * @since 1.0
 */
public class SimpleDBPersistentEntity extends AbstractPersistentEntity {

    public SimpleDBPersistentEntity(Class javaClass, MappingContext context) {
        super(javaClass, context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ClassMapping<Family> getMapping() {
        return new KeyValueClassMapping(this, context);
    }

    public class KeyValueClassMapping extends AbstractClassMapping<Family> {
        public KeyValueClassMapping(PersistentEntity entity, MappingContext context) {
            super(entity, context);
        }
        @Override
        public Family getMappedForm() {
            return (Family) context.getMappingFactory().createMappedForm(SimpleDBPersistentEntity.this);
        }
    }

}