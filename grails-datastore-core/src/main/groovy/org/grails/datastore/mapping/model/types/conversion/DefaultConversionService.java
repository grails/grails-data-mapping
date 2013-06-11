package org.grails.datastore.mapping.model.types.conversion;

import groovy.lang.GroovyObject;

import java.io.Serializable;

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
        // force converting GStringImpl & StreamCharBuffer to String before conversion
        if(source instanceof CharSequence && source.getClass() != String.class && 
                targetType != null && targetType.getType() != source.getClass()) {
            source = source.toString();
            sourceType = TypeDescriptor.valueOf(String.class);
        }
        return super.convert(source, sourceType, targetType);
    }

    @Override
    public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
        // fix EnumToString conversions for Enums implemented in Groovy
        // see org.springframework.core.convert.support.EnumToStringConverter.match method
        if (targetType != null 
                && targetType.getType() == String.class
                && sourceType != null 
                && (sourceType.getType() == GroovyObject.class || 
                    sourceType.getType() == Comparable.class || 
                    sourceType.getType() == Serializable.class)) {
            return false;
        }
        boolean reply = super.canConvert(sourceType, targetType);
        if(!reply && sourceType != null && CharSequence.class.isAssignableFrom(sourceType.getType())) {
            reply = super.canConvert(TypeDescriptor.valueOf(String.class), targetType);
        }
        return reply;
    }
}
