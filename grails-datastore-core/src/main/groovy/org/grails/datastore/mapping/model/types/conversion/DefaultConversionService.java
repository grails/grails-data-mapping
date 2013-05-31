package org.grails.datastore.mapping.model.types.conversion;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.datetime.DateFormatterRegistrar;

/**
 * Default conversion service th
 * @author Graeme Rocher
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DefaultConversionService extends org.springframework.core.convert.support.DefaultConversionService {
    
    public DefaultConversionService() {
        DateFormatterRegistrar.addDateConverters(this);
        
        addConverter(new StringToShortConverter());
        addConverter(new StringToBigIntegerConverter());
        addConverter(new StringToBigDecimalConverter());
        addConverter(new StringToCurrencyConverter());
        addConverter(new StringToLocaleConverter());
        addConverter(new StringToTimeZoneConverter());
        addConverter(new StringToURLConverter());
        addConverter(new IntArrayToIntegerArrayConverter());
        addConverter(new LongArrayToLongArrayConverter());
        addConverter(new IntegerToByteConverter());
        addConverter(new DoubleToFloatConverter());
        addConverter(new IntegerToShortConverter());
    }

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
