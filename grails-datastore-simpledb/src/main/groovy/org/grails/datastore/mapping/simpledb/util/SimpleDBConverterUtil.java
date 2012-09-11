package org.grails.datastore.mapping.simpledb.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.springframework.core.convert.ConversionService;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.proxy.EntityProxy;

/**
 * Simple conversion utility for SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBConverterUtil {
    public static String convertToString(Object value, MappingContext mappingContext) {
        if (value == null) {
            return null;
        }
        String stringValue = null;
        if (value instanceof String) {
            stringValue = (String)value;
        } else if (shouldConvert(value, mappingContext)) {
            final ConversionService conversionService = mappingContext.getConversionService();
            stringValue = conversionService.convert(value, String.class);
        }
        return stringValue;
    }

    public static Collection<String> convertToStrings(Collection<?> values, MappingContext mappingContext) {
        List<String> stringValues = new LinkedList<String>();
        for (Object value : values) {
            stringValues.add(convertToString(value, mappingContext));
        }

        return stringValues;
    }

    private static boolean shouldConvert(Object value, MappingContext mappingContext) {
        return !mappingContext.isPersistentEntity(value) && !(value instanceof EntityProxy);
    }
}
