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
        // force converting GStringImpl & StreamCharBuffer to String before conversion if no conversion exists
        if(source instanceof CharSequence && !super.canConvert(sourceType, targetType)) {
            source = source.toString();
            sourceType = TypeDescriptor.valueOf(String.class);
        }
        return super.convert(source, sourceType, targetType);
    }

    @Override
    public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
        boolean reply = super.canConvert(sourceType, targetType);
        if(!reply && sourceType != null && CharSequence.class.isAssignableFrom(sourceType.getType())) {
            reply = super.canConvert(TypeDescriptor.valueOf(String.class), targetType);
        }
        return reply;
    }
}
