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

import groovy.lang.MetaBeanProperty;
import groovy.lang.MetaClass;
import groovy.lang.MetaMethod;
import groovy.lang.MetaProperty;
import org.codehaus.groovy.reflection.CachedField;
import org.codehaus.groovy.reflection.CachedMethod;
import org.codehaus.groovy.reflection.ClassInfo;
import org.springframework.beans.BeanUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Reads the properties of a class in an optimized manner avoiding exceptions.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class ClassPropertyFetcher {

    private final Class clazz;
    private final ClassInfo classInfo;
    private final MetaClass theMetaClass;

    public static ClassPropertyFetcher forClass(final Class c) {
        return new ClassPropertyFetcher(c);
    }

    /**
     * @deprecated Does nothing, no longer needed
     */
    @Deprecated
    public static void clearCache()  {
        // no-op
    }

    ClassPropertyFetcher(final Class clazz) {
        this.clazz = clazz;
        this.classInfo = ClassInfo.getClassInfo(clazz);
        this.theMetaClass = classInfo.getMetaClass();
    }

    /**
     * @return The Java that this ClassPropertyFetcher was constructor for
     */
    public Class getJavaClass() {
        return clazz;
    }

    /**
     * @Deprecated will be removed in a future version of GORM
     */
    @Deprecated
    public Object getReference() {
        return BeanUtils.instantiate(clazz);
    }

    /**
     * @deprecated  Use getMetaProperties instead
     */
    @Deprecated
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            return Introspector.getBeanInfo(clazz).getPropertyDescriptors();
        } catch (IntrospectionException e) {
            return new PropertyDescriptor[0];
        }
    }

    /**
     * @return The meta properties of this class
     */
    public List<MetaProperty> getProperties() {
        return theMetaClass.getProperties();
    }

    public boolean isReadableProperty(String name) {
        MetaProperty metaProperty = theMetaClass.getMetaProperty(name);
        if(metaProperty instanceof MetaBeanProperty) {
            MetaBeanProperty metaBeanProperty = (MetaBeanProperty) metaProperty;
            return metaBeanProperty.getField() != null || metaBeanProperty.getGetter() != null;
        }
        return false;
    }

    public Object getPropertyValue(String name) {
        MetaProperty metaProperty = theMetaClass.getMetaProperty(name);
        if(metaProperty != null && Modifier.isStatic(metaProperty.getModifiers())) {
            return metaProperty.getProperty(classInfo.getCachedClass().getTheClass());
        }
        return null;
    }

    public Object getPropertyValue(final Object instance, String name) {
        MetaProperty metaProperty = theMetaClass.getMetaProperty(name);
        if(metaProperty != null && !Modifier.isStatic(metaProperty.getModifiers())) {
            return metaProperty.getProperty(instance);
        }
        return null;
    }

    public <T> T getStaticPropertyValue(String name, Class<T> c) {
        return returnOnlyIfInstanceOf(getPropertyValue(name), c);
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
        Collection<ClassInfo> hierarchy = classInfo.getCachedClass().getHierarchy();
        List<T> values = new ArrayList<>(4);
        for (ClassInfo current : hierarchy) {
            MetaProperty metaProperty = current.getMetaClass().getMetaProperty(name);
            if(metaProperty != null) {
                Object val = metaProperty.getProperty(current.getCachedClass().getTheClass());
                if(c.isInstance(val)) {
                    values.add((T) val);
                }
            }
            else {
                // reached a super class that doesn't have the property
                break;
            }
        }
        Collections.reverse(values);
        return values;
    }

    public <T> T getPropertyValue(String name, Class<T> c) {
        return getStaticPropertyValue(name, c);
    }

    public Class getPropertyType(String name) {
        return getPropertyType(name, false);
    }

    public Class getPropertyType(String name, boolean onlyInstanceProperties) {
        MetaProperty metaProperty = theMetaClass.getMetaProperty(name);
        if(metaProperty != null) {
            boolean isStatic = Modifier.isStatic(metaProperty.getModifiers());
            if(onlyInstanceProperties && !isStatic) {
                return metaProperty.getType();
            }
            else if(!onlyInstanceProperties && isStatic) {
                return metaProperty.getType();
            }
        }
        return null;
    }

    public PropertyDescriptor getPropertyDescriptor(String name) {
        MetaProperty property = theMetaClass.getMetaProperty(name);
        if(property instanceof MetaBeanProperty) {
            MetaBeanProperty beanProperty = (MetaBeanProperty) property;
            MetaMethod getter = beanProperty.getGetter();
            MetaMethod setter = beanProperty.getSetter();
            if(getter instanceof CachedMethod && setter instanceof CachedMethod) {
                CachedMethod cachedGetter = (CachedMethod) getter;
                CachedMethod cachedSetter = (CachedMethod) setter;
                try {
                    return new PropertyDescriptor(beanProperty.getName(), cachedGetter.getCachedMethod(), cachedSetter.getCachedMethod());
                } catch (IntrospectionException e) {
                    // ignore
                }
            }
            else if(getter instanceof CachedMethod) {
                CachedMethod cachedGetter = (CachedMethod) getter;
                try {
                    return new PropertyDescriptor(beanProperty.getName(), cachedGetter.getCachedMethod(), null);
                } catch (IntrospectionException e) {
                    // ignore
                }

            }
        }
        return null;
    }

    public List<PropertyDescriptor> getPropertiesOfType(Class javaClass) {
        List<MetaProperty> properties = theMetaClass.getProperties();
        List<PropertyDescriptor> propertyDescriptors = new ArrayList<>(2);

        for (MetaProperty property : properties) {
            int modifiers = property.getModifiers();
            if(Modifier.isStatic(modifiers) || property.getName().contains("$") || !property.getType().equals(javaClass)) continue;

            addBeanProperty(propertyDescriptors, property);
        }
        return propertyDescriptors;
    }

    @SuppressWarnings("unchecked")
    public List<PropertyDescriptor> getPropertiesAssignableToType(Class assignableType) {
        List<MetaProperty> properties = theMetaClass.getProperties();
        List<PropertyDescriptor> propertyDescriptors = new ArrayList<>(2);
        for (MetaProperty property : properties) {
            int modifiers = property.getModifiers();
            if(Modifier.isStatic(modifiers) || property.getName().contains("$") || !assignableType.isAssignableFrom(property.getType())) {
                continue;
            }

            addBeanProperty(propertyDescriptors, property);
        }
        return propertyDescriptors;
    }

    @SuppressWarnings("unchecked")
    public List<PropertyDescriptor> getPropertiesAssignableFromType(Class assignableType) {
        List<MetaProperty> properties = theMetaClass.getProperties();
        List<PropertyDescriptor> propertyDescriptors = new ArrayList<>(2);
        for (MetaProperty property : properties) {
            int modifiers = property.getModifiers();
            if(Modifier.isStatic(modifiers) || property.getName().contains("$") || !property.getType().equals(assignableType)) continue;
            addBeanProperty(propertyDescriptors, property);
        }
        return propertyDescriptors;
    }

    public Field getDeclaredField(String name) {
        MetaProperty metaProperty = theMetaClass.getMetaProperty(name);
        if(metaProperty instanceof MetaBeanProperty) {
            CachedField field = ((MetaBeanProperty) metaProperty).getField();
            if(field != null) {
                return field.field;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T returnOnlyIfInstanceOf(Object value, Class<T> type) {
        if (value != null && (type == Object.class || ReflectionUtils.isAssignableFrom(type, value.getClass()))) {
            return (T)value;
        }
        return null;
    }

    protected void addBeanProperty(List<PropertyDescriptor> propertyDescriptors, MetaProperty property) {
        if(property instanceof MetaBeanProperty) {
            MetaBeanProperty beanProperty = (MetaBeanProperty) property;
            MetaMethod getter = beanProperty.getGetter();
            MetaMethod setter = beanProperty.getSetter();
            boolean isGetterCachedMethod = getter instanceof CachedMethod;
            if(isGetterCachedMethod && setter instanceof CachedMethod) {
                CachedMethod cachedGetter = (CachedMethod) getter;
                CachedMethod cachedSetter = (CachedMethod) setter;
                try {
                    propertyDescriptors.add(new PropertyDescriptor(beanProperty.getName(), cachedGetter.getCachedMethod(), cachedSetter.getCachedMethod()));
                } catch (IntrospectionException e) {
                    // ignore
                }
            }
            else if(isGetterCachedMethod) {
                CachedMethod cachedGetter = (CachedMethod) getter;
                try {
                    propertyDescriptors.add(new PropertyDescriptor(beanProperty.getName(), cachedGetter.getCachedMethod(), null));
                } catch (IntrospectionException e) {
                    // ignore
                }

            }
        }
    }
}
