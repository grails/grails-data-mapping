package org.grails.datastore.gorm.cassandra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.springframework.core.convert.ConversionService;

/**
 * Override convertArguments implementation to prevent conversion if conversionService cannot convert, just like the MethodExpression implementation.
 * Prevents conversion of composite primary key maps and other types such as Enum, URL etc.
 * which will be converted in CassandraQuery using CassandraEntityPersister
 *      
 */
public class InList extends org.grails.datastore.gorm.finders.MethodExpression.InList{

    public InList(Class<?> targetClass, String propertyName) {
        super(targetClass, propertyName);       
    }

    @Override
    public void convertArguments(PersistentEntity persistentEntity) {
        ConversionService conversionService = persistentEntity
                .getMappingContext().getConversionService();
        PersistentProperty<?> prop = persistentEntity
                .getPropertyByName(propertyName);
        if (prop == null) {
            if (propertyName.equals(persistentEntity.getIdentity().getName())) {
                prop = persistentEntity.getIdentity();
            }
        }
        if (prop != null) {
            Class<?> type = prop.getType();
            Collection<?> collection = (Collection<?>) arguments[0];
            List<Object> converted = new ArrayList<Object>(collection.size());
            for (Object o : collection) {
                if (o != null && !type.isAssignableFrom(o.getClass()) && conversionService.canConvert(o.getClass(), type)) {
                    o = conversionService.convert(o, type);
                }
                converted.add(o);
            }
            arguments[0] = converted;
        }
    }

}
