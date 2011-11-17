package org.grails.datastore.mapping.model.types.conversion;

import org.springframework.core.convert.support.GenericConversionService;

/**
 * Default conversion service th
 * @author Graeme Rocher
 *
 */
public class DefaultConversionService extends GenericConversionService{

    @Override
    public <T> T convert(Object source, Class<T> targetType) {
        if(targetType.isEnum() && source instanceof CharSequence) {
             return (T) Enum.valueOf((Class)targetType, source.toString());
        }
        else {
            return super.convert(source, targetType);
        }
    }
}
