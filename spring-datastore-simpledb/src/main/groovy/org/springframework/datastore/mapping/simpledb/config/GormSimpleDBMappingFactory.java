package org.springframework.datastore.mapping.simpledb.config;

import groovy.lang.Closure;
import org.springframework.datastore.mapping.config.groovy.MappingConfigurationBuilder;
import org.springframework.datastore.mapping.keyvalue.mapping.config.Family;
import org.springframework.datastore.mapping.keyvalue.mapping.config.GormKeyValueMappingFactory;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.config.GormProperties;
import org.springframework.datastore.mapping.reflect.ClassPropertyFetcher;

/**
 * MappingFactory for SimpleDB
 *
 * @author Roman Stepanenko
 * @since 0.l
 */
public class GormSimpleDBMappingFactory extends GormKeyValueMappingFactory {
    public GormSimpleDBMappingFactory() {
        super(null);
    }

    @Override
    public Family createMappedForm(PersistentEntity entity) {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(entity.getJavaClass());

        Closure value = cpf.getStaticPropertyValue(GormProperties.MAPPING, Closure.class);
        if (value == null) {
            return new SimpleDBDomainClassMappedForm(entity.getName());
        }

        Family family = new SimpleDBDomainClassMappedForm();
        MappingConfigurationBuilder builder = new MappingConfigurationBuilder(family, getPropertyMappedFormType());

        builder.evaluate(value);
        value = cpf.getStaticPropertyValue(GormProperties.CONSTRAINTS, Closure.class);
        if (value != null) {
            builder.evaluate(value);
        }
        entityToPropertyMap.put(entity, builder.getProperties());
        return family;
    }
}
