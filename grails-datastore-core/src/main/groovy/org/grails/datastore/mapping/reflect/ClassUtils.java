/*
 * Copyright 2015 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.mapping.reflect;

import java.util.List;
import java.util.Map;

/**
 * Helper methods for dealing with classes and reflection
 *
 * @author Graeme Rocher
 * @since 5.0
 */
public class ClassUtils {

    /**
     * Determine whether the {@link Class} identified by the supplied name is present
     * and can be loaded. Will return {@code false} if either the class or
     * one of its dependencies is not present or cannot be loaded.
     * @param className the name of the class to check
     * (may be {@code null}, which indicates the default class loader)
     * @return whether the specified class is present
     */
    public static boolean isPresent(String className) {
        try {
            Class.forName(className, false, ClassUtils.class.getClassLoader());
            return true;
        }
        catch (Throwable ex) {
            // Class or one of its dependencies is not present...
            return false;
        }
    }
    /**
     * Determine whether the {@link Class} identified by the supplied name is present
     * and can be loaded. Will return {@code false} if either the class or
     * one of its dependencies is not present or cannot be loaded.
     * @param className the name of the class to check
     * @param classLoader the class loader to use
     * (may be {@code null}, which indicates the default class loader)
     * @return whether the specified class is present
     */
    public static boolean isPresent(String className, ClassLoader classLoader) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        }
        catch (Throwable ex) {
            // Class or one of its dependencies is not present...
            return false;
        }
    }

    /**
     * Retrieves a boolean value from a Map for the given key
     *
     * @param key The key that references the boolean value
     * @param map The map to look in
     * @return A boolean value which will be false if the map is null, the map doesn't contain the key or the value is false
     */
    public static boolean getBooleanFromMap(String key, Map<?, ?> map) {
        if (map == null) return false;
        if (map.containsKey(key)) {
            Object o = map.get(key);
            if (o == null)return false;
            if (o instanceof Boolean) {
                return (Boolean)o;
            }
            return Boolean.valueOf(o.toString());
        }
        return false;
    }

    public static boolean isClassBelowPackage(Class<?> domainClazz, List packageList) {
        String classPackage = domainClazz.getPackage().getName();
        for (Object packageName : packageList) {
            if (packageName != null) {
                if (classPackage.startsWith(packageName.toString())) {
                    return true;
                }
            }
        }
        return false;

    }
}
