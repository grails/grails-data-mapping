package org.grails.datastore.mapping.model.types.conversion;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.GenericConversionService;

/**
 * Default conversion service th
 * @author Graeme Rocher
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DefaultConversionService extends GenericConversionService {

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (targetType.getType().isEnum() && source instanceof CharSequence) {
             return Enum.valueOf((Class)targetType.getType(), source.toString());
        }
        if (targetType.getType().equals(String.class) && source instanceof Enum) {
            return ((Enum)source).name();
        }
        return super.convert(source, sourceType, targetType);
    }

    @Override
    public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return (targetType.getType().isEnum() && CharSequence.class.isAssignableFrom(sourceType.getType())) ||
                (targetType.getType().equals(String.class) && sourceType.getType().isEnum()) ||
                super.canConvert(sourceType, targetType);
    }
}
