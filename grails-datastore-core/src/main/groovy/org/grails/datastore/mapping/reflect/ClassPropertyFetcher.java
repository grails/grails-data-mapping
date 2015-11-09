/* Copyright 2004-2005 the original author or authors.
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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Reads the properties of a class in an optimized manner avoiding exceptions.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class ClassPropertyFetcher {

    private final Class clazz;
    // static fetchers for this class, but also for all super classes with the property.
    // first item in each list is most derived version of static property.
    final Map<String, List<PropertyFetcher>> staticFetchers = new HashMap<String, List<PropertyFetcher>>();
    final Map<String, PropertyFetcher> instanceFetchers = new HashMap<String, PropertyFetcher>();
    private final ReferenceInstanceCallback callback;
    private PropertyDescriptor[] propertyDescriptors;
    private Map<String, PropertyDescriptor> propertyDescriptorsByName = new HashMap<String, PropertyDescriptor>();
    private Map<String, Field> fieldsByName = new HashMap<String, Field>();
    private Map<Class, List<PropertyDescriptor>> typeToPropertyMap = new HashMap<Class, List<PropertyDescriptor>>();

    private static Map<Class, ClassPropertyFetcher> cachedClassPropertyFetchers = new WeakHashMap<Class, ClassPropertyFetcher>();

    public static ClassPropertyFetcher forClass(final Class c) {
        ClassPropertyFetcher cpf = cachedClassPropertyFetchers.get(c);
        if (cpf == null) {
            cpf = new ClassPropertyFetcher(c);
            cachedClassPropertyFetchers.put(c, cpf);
        }
        return cpf;
    }

    public static void clearCache()  {
        cachedClassPropertyFetchers.clear();
    }

    ClassPropertyFetcher(final Class clazz) {
        this.clazz = clazz;
        this.callback = new ReferenceInstanceCallback() {
            public Object getReferenceInstance() {
               return ReflectionUtils.instantiate(clazz);
            }
        };
        init();
    }

    /**
     * @return The Java that this ClassPropertyFetcher was constructor for
     */
    public Class getJavaClass() {
        return clazz;
    }

    public Object getReference() {
        return callback == null ? null : callback.getReferenceInstance();
    }

    public PropertyDescriptor[] getPropertyDescriptors() {
        return propertyDescriptors;
    }

    public boolean isReadableProperty(String name) {
        return staticFetchers.containsKey(name) ||
               instanceFetchers.containsKey(name);
    }

    private void init() {

        List<Class> allClasses = resolveAllClasses(clazz);
        for (Class c : allClasses) {
            Field[] fields = c.getDeclaredFields();
            for (Field field : fields) {
                processField(field);
            }
            Method[] methods = c.getDeclaredMethods();
            for (Method method : methods) {
                processMethod(method);
            }
        }

        try {
            propertyDescriptors = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
        } catch (IntrospectionException e) {
            // ignore
        }

        if (propertyDescriptors == null) {
            return;
        }

        for (PropertyDescriptor desc : propertyDescriptors) {
            propertyDescriptorsByName.put(desc.getName(),desc);
            final Class<?> propertyType = desc.getPropertyType();
            if (propertyType == null) continue;
            List<PropertyDescriptor> pds = typeToPropertyMap.get(propertyType);
            if (pds == null) {
                pds = new ArrayList<PropertyDescriptor>();
                typeToPropertyMap.put(propertyType, pds);
            }
            pds.add(desc);

            Method readMethod = desc.getReadMethod();
            if (readMethod != null) {
                boolean staticReadMethod = Modifier.isStatic(readMethod.getModifiers());
                if (staticReadMethod) {
                    List<PropertyFetcher> propertyFetchers = staticFetchers.get(desc.getName());
                    if (propertyFetchers == null) {
                        staticFetchers.put(desc.getName(), propertyFetchers = new ArrayList<PropertyFetcher>());
                    }
                    propertyFetchers.add(
                            new GetterPropertyFetcher(readMethod, true));
                } else {
                    instanceFetchers.put(desc.getName(),
                            new GetterPropertyFetcher(readMethod, false));
                }
            }
        }
    }

    private void processMethod(Method method) {
        if (method.isSynthetic()) {
            return;
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            return;
        }
        if (Modifier.isStatic(method.getModifiers()) &&
                method.getReturnType() != Void.class) {
            if (method.getParameterTypes().length == 0) {
                String name = method.getName();
                if (name.indexOf('$') == -1) {
                    final String propertyName = NameUtils.getPropertyNameForGetterOrSetter(name);
                    if(propertyName != null) {
                        name = propertyName;
                    }
                    PropertyFetcher fetcher = new GetterPropertyFetcher(method, true);
                    List<PropertyFetcher> propertyFetchers = staticFetchers.get(name);
                    if (propertyFetchers == null) {
                        staticFetchers.put(name, propertyFetchers = new ArrayList<PropertyFetcher>());
                    }
                    propertyFetchers.add(fetcher);
                    String decapitalized = Introspector.decapitalize(name);
                    if (!decapitalized.equals(name)) {
                        propertyFetchers = staticFetchers.get(decapitalized);
                        if (propertyFetchers == null) {
                            staticFetchers.put(decapitalized, propertyFetchers = new ArrayList<PropertyFetcher>());
                        }
                        propertyFetchers.add(fetcher);
                    }
                }
            }
        }
    }

    private void processField(Field field) {
        if (field.isSynthetic()) {
            return;
        }
        final int modifiers = field.getModifiers();
        final String name = field.getName();
        if (!Modifier.isPublic(modifiers)) {
            if (name.indexOf('$') == -1) {
                fieldsByName.put(name, field);
            }
        }
        else {
            if (name.indexOf('$') == -1) {
                boolean staticField = Modifier.isStatic(modifiers);
                if (staticField) {
                    List<PropertyFetcher> propertyFetchers = staticFetchers.get(name);
                    if (propertyFetchers == null) {
                        staticFetchers.put(name, propertyFetchers = new ArrayList<PropertyFetcher>());
                    }
                    propertyFetchers.add(new FieldReaderFetcher(field, staticField));
                } else {
                    instanceFetchers.put(name, new FieldReaderFetcher(field, staticField));
                }
            }
        }
    }

    private List<Class> resolveAllClasses(Class c) {
        List<Class> list = new ArrayList<Class>();
        Class currentClass = c;
        while (currentClass != null) {
            list.add(currentClass);
            currentClass = currentClass.getSuperclass();
        }
        Collections.reverse(list);
        return list;
    }

    public Object getPropertyValue(String name) {
        return getPropertyValue(name, false);
    }

    public Object getPropertyValue(String name, boolean onlyInstanceProperties) {
        PropertyFetcher fetcher = resolveFetcher(name, onlyInstanceProperties);
        return getPropertyValueWithFetcher(name, fetcher);
    }

    public Object getPropertyValue(final Object instance, String name) {
        PropertyFetcher fetcher = resolveFetcher(name, true);
        return getPropertyValueWithFetcher(name, fetcher,new ReferenceInstanceCallback() {
            @Override
            public Object getReferenceInstance() {
                return instance;
            }
        });
    }

    private Object getPropertyValueWithFetcher(String name, PropertyFetcher fetcher) {
        ReferenceInstanceCallback thisCallback = callback;
        return getPropertyValueWithFetcher(name, fetcher, thisCallback);
    }

    private Object getPropertyValueWithFetcher(String name, PropertyFetcher fetcher, ReferenceInstanceCallback thisCallback) {
        if (fetcher != null) {
            try {
                return fetcher.get(thisCallback);
            } catch (Exception e) {
                System.err.println("Error fetching property's "
                        + name + " value from class " + clazz.getName() + ": " + e.getMessage());
            }
        }
        return null;
    }

    public <T> T getStaticPropertyValue(String name, Class<T> c) {
        List<PropertyFetcher> propertyFetchers = staticFetchers.get(name);
        if (propertyFetchers == null) {
            return null;
        }
        PropertyFetcher fetcher = propertyFetchers.get(0);

        Object v = getPropertyValueWithFetcher(name, fetcher);
        return returnOnlyIfInstanceOf(v, c);
    }

    /**
     * Get the list of property values for this static property from the whole inheritance hierarchy, starting
     * from the most derived version of the property ending with the base class. There are entries for each extant
     * version of the property in turn, so if you have a 10-deep inheritance hierarchy, you may get 0+ values returned,
     * one per class in the hierarchy that has the property declared (and of the correct type).
     * @param name Name of the property.
     * @param c Required type of the property (including derived types)
     * @param <T> Required type of the property.
     * @return The list, with 0+ values (never null). Do not modify the returned list.
     */
    public <T> List<T> getStaticPropertyValuesFromInheritanceHierarchy(String name, Class<T> c) {
        List<PropertyFetcher> propertyFetchers = staticFetchers.get(name);
        if (propertyFetchers == null) {
            return Collections.emptyList();
        }
        List<T> values = new ArrayList<T>(propertyFetchers.size());
        for (PropertyFetcher fetcher : propertyFetchers) {
            Object v = getPropertyValueWithFetcher(name, fetcher);
            T t = returnOnlyIfInstanceOf(v, c);
            if (t != null) {
                values.add(t);
            }
        }
        return values;
    }

    public <T> T getPropertyValue(String name, Class<T> c) {
        return returnOnlyIfInstanceOf(getPropertyValue(name, false), c);
    }

    @SuppressWarnings("unchecked")
    private <T> T returnOnlyIfInstanceOf(Object value, Class<T> type) {
        if (value != null && (type == Object.class || ReflectionUtils.isAssignableFrom(type, value.getClass()))) {
            return (T)value;
        }
        return null;
    }

    private PropertyFetcher resolveFetcher(String name, boolean onlyInstanceProperties) {
        PropertyFetcher fetcher = null;
        if (!onlyInstanceProperties) {
            List<PropertyFetcher> f = staticFetchers.get(name);
            fetcher = f == null ? null : f.get(0);
        }
        if (fetcher == null) {
            fetcher = instanceFetchers.get(name);
        }
        return fetcher;
    }

    public Class getPropertyType(String name) {
        return getPropertyType(name, false);
    }

    public Class getPropertyType(String name, boolean onlyInstanceProperties) {
        PropertyFetcher fetcher = resolveFetcher(name, onlyInstanceProperties);
        return fetcher == null ? null : fetcher.getPropertyType(name);
    }

    public PropertyDescriptor getPropertyDescriptor(String name) {
        return propertyDescriptorsByName.get(name);
    }

    public List<PropertyDescriptor> getPropertiesOfType(Class javaClass) {
        final List<PropertyDescriptor> propertyDescriptorList = typeToPropertyMap.get(javaClass);
        if (propertyDescriptorList == null) return Collections.emptyList();
        return propertyDescriptorList;
    }

    @SuppressWarnings("unchecked")
    public List<PropertyDescriptor> getPropertiesAssignableToType(Class assignableType) {
        List<PropertyDescriptor> properties = new ArrayList<PropertyDescriptor>();
        for (Class type : typeToPropertyMap.keySet()) {
            if (assignableType.isAssignableFrom(type)) {
                properties.addAll(typeToPropertyMap.get(type));
            }
        }
        return properties;
    }

    @SuppressWarnings("unchecked")
    public List<PropertyDescriptor> getPropertiesAssignableFromType(Class assignableType) {
        List<PropertyDescriptor> properties = new ArrayList<PropertyDescriptor>();
        for (Class type : typeToPropertyMap.keySet()) {
            if (type.isAssignableFrom( assignableType )) {
                properties.addAll(typeToPropertyMap.get(type));
            }
        }
        return properties;
    }

    public interface ReferenceInstanceCallback {
        Object getReferenceInstance();
    }

    interface PropertyFetcher {
        Object get(ReferenceInstanceCallback callback)
                throws IllegalArgumentException, IllegalAccessException,
                InvocationTargetException;

        Class getPropertyType(String name);
    }

    static class GetterPropertyFetcher implements PropertyFetcher {
        private final Method readMethod;
        private final boolean staticMethod;

        GetterPropertyFetcher(Method readMethod, boolean staticMethod) {
            this.readMethod = readMethod;
            this.staticMethod = staticMethod;
            ReflectionUtils.makeAccessible(readMethod);
        }

        public Object get(ReferenceInstanceCallback callback)
                throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            if (staticMethod) {
                return readMethod.invoke(null, (Object[]) null);
            }
            if (callback != null) {
                return readMethod.invoke(callback.getReferenceInstance(), (Object[]) null);
            }
            return null;
        }

        public Class getPropertyType(String name) {
            return readMethod.getReturnType();
        }
    }

    static class FieldReaderFetcher implements PropertyFetcher {
        private final Field field;
        private final boolean staticField;

        public FieldReaderFetcher(Field field, boolean staticField) {
            this.field = field;
            this.staticField = staticField;
            ReflectionUtils.makeAccessible(field);
        }

        public Object get(ReferenceInstanceCallback callback)
                throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            if (staticField) {
                return field.get(null);
            }
            if (callback != null) {
                return field.get(callback.getReferenceInstance());
            }
            return null;
        }

        public Class getPropertyType(String name) {
            return field.getType();
        }
    }

    public Field getDeclaredField(String name) {
        return fieldsByName.get(name);
    }
}
