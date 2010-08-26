package org.springframework.datastore.config.utils;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;

import java.io.Serializable;
import java.util.Map;

/**
 * Used to ease reading of configuration
 *
 */
public class ConfigUtils {


    private static TypeConverter converter = new SimpleTypeConverter();
    public static <T> T read(Class<T> type, String key, Map<String, String> config, T defaultValue) {
        String value = config.get(key);

        if(value != null) {
            return converter.convertIfNecessary(value, type);
        }
        return defaultValue;
    }
}
