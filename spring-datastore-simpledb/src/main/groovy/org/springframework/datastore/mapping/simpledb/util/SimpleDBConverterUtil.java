package org.springframework.datastore.mapping.simpledb.util;

import org.springframework.core.convert.ConversionService;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.proxy.EntityProxy;

/**
 * Simple conversion utility for SimpleDB. 
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBConverterUtil {
    public static String convertToString(Object value, MappingContext mappingContext) {
        String stringValue = null;
        if ( value instanceof String ) {
            stringValue = (String)value;
        } else if (shouldConvert(value, mappingContext)) {
            final ConversionService conversionService = mappingContext.getConversionService();
            stringValue = conversionService.convert(value, String.class);
        }
        return stringValue;
    }

    private static boolean shouldConvert(Object value, MappingContext mappingContext) {
        return !mappingContext.isPersistentEntity(value) && !(value instanceof EntityProxy);
    }

}
