/* Copyright (C) 2011 SpringSource
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
package org.grails.datastore.mapping.engine.internal;

import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.PropertyMapping;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

/**
 * Utility methods for mapping logic.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class MappingUtils {

    public static String getTargetKey(@SuppressWarnings("rawtypes") PersistentProperty property) {
        @SuppressWarnings("unchecked")
        PropertyMapping<Property> mapping = property.getMapping();
        String targetName;

        if (mapping != null && mapping.getMappedForm() != null) {
            String tmp = mapping.getMappedForm().getTargetName();
            targetName = tmp != null ? tmp : property.getName();
        }
        else {
            targetName = property.getName();
        }
        return targetName;
    }

    /**
     * Creates a concrete collection for the suppied interface
     * @param interfaceType The interface
     * @return ArrayList for List, TreeSet for SortedSet, HashSet for Set etc.
     */
    @SuppressWarnings("rawtypes")
    public static Collection createConcreteCollection(Class interfaceType) {
        Collection elements;
        if (interfaceType.equals(List.class)) {
            elements = new ArrayList();
        }
        else if (interfaceType.equals(SortedSet.class)) {
            elements = new TreeSet();
        }
        else if (interfaceType.equals(Queue.class)) {
            elements = new ArrayDeque();
        }
        else {
            elements = new HashSet();
        }
        return elements;
    }

    public static Class getGenericTypeForProperty(Class javaClass, String propertyName) {
        Class genericClass = null;

        try {
            Field declaredField = javaClass.getDeclaredField(propertyName);
            Type genericType = declaredField.getGenericType();
            if(genericType instanceof ParameterizedType) {
                Type[] typeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
                if(typeArguments.length>0) {
                    genericClass = (Class) typeArguments[0];
                }
            }
        } catch (NoSuchFieldException e) {
            // ignore
        }
        return genericClass;
    }

    public static Class getGenericType(Class propertyType) {
        Class genericType = null;
        TypeVariable[] typeParameters = propertyType.getTypeParameters();
        if(typeParameters != null && typeParameters.length>0) {
            Type[] bounds = typeParameters[0].getBounds();
            if(bounds != null && bounds.length>0 && (bounds[0] instanceof Class)) {
                genericType = (Class) bounds[0];
            }
        }
        return genericType;
    }
}
