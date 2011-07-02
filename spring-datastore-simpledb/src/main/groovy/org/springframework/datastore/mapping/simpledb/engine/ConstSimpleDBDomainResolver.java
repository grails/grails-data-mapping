package org.springframework.datastore.mapping.simpledb.engine;

import org.springframework.datastore.mapping.keyvalue.mapping.config.Family;
import org.springframework.datastore.mapping.model.ClassMapping;
import org.springframework.datastore.mapping.model.PersistentEntity;


/**
 * An implementation of the domain resolver which assumes there is no sharding - i.e. always
 * the same domain name for all the primary keys (for the same type of {@link org.springframework.datastore.mapping.model.PersistentEntity}
 */
public class ConstSimpleDBDomainResolver extends AbstractSimpleDBDomainResolver {
    public ConstSimpleDBDomainResolver(PersistentEntity entity) {
        super(entity);
    }

    public String resolveDomain(String id) {
        return entityFamily; //without sharding it is always the same one per PersistentEntity
    }
}
