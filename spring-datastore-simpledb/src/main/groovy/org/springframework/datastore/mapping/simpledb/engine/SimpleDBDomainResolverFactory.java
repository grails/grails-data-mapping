package org.springframework.datastore.mapping.simpledb.engine;

import org.springframework.datastore.mapping.model.ClassMapping;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.simpledb.SimpleDBDatastore;
import org.springframework.datastore.mapping.simpledb.config.SimpleDBDomainClassMappedForm;

/**
 * Encapsulates logic of building appropriately configured SimpleDBDomainResolver instance.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBDomainResolverFactory {
    public SimpleDBDomainResolver buildResolver(PersistentEntity entity, SimpleDBDatastore simpleDBDatastore){
        ClassMapping<SimpleDBDomainClassMappedForm> classMapping = (ClassMapping<SimpleDBDomainClassMappedForm>) entity.getMapping();
        SimpleDBDomainClassMappedForm mappedForm = classMapping.getMappedForm();
        String entityFamily = getFamily(entity, mappedForm);

        if ( mappedForm.isShardingEnabled() ) {
            throw new RuntimeException("sharding is not implemented yet");
        } else {
            return new ConstSimpleDBDomainResolver(entityFamily, simpleDBDatastore.getDomainNamePrefix());
        }
    }

    protected String getFamily(PersistentEntity persistentEntity, SimpleDBDomainClassMappedForm mappedForm) {
        String table = null;
        if (mappedForm != null) {
            table = mappedForm.getFamily();
        }
        if (table == null) table = persistentEntity.getJavaClass().getName();
        return table;
    }


}
