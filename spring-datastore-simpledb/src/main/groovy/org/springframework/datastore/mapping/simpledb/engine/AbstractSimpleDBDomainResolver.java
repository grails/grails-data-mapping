package org.springframework.datastore.mapping.simpledb.engine;

import org.springframework.datastore.mapping.keyvalue.mapping.config.Family;
import org.springframework.datastore.mapping.model.ClassMapping;
import org.springframework.datastore.mapping.model.PersistentEntity;

public abstract class AbstractSimpleDBDomainResolver implements SimpleDBDomainResolver {
    public AbstractSimpleDBDomainResolver(PersistentEntity entity, String domainNamePrefix) {
        ClassMapping classMapping = entity.getMapping();
        entityFamily = getFamily(entity, classMapping);
        this.domainNamePrefix = domainNamePrefix;
        if ( domainNamePrefix != null ) {
            entityFamily = domainNamePrefix + entityFamily;
        }
    }

    protected String getFamily(PersistentEntity persistentEntity, ClassMapping<Family> cm) {
        String table = null;
        if (cm.getMappedForm() != null) {
            table = cm.getMappedForm().getFamily();
        }
        if (table == null) table = persistentEntity.getJavaClass().getName();
        return table;
    }

    protected String entityFamily;
    protected String domainNamePrefix;
}
