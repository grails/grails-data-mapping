/* Copyright (C) 2010 SpringSource
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
package org.springframework.datastore.mapping.engine;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.ConversionService;
import org.springframework.datastore.mapping.model.ClassMapping;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;


/**
 * Class used to access properties of an entity. Also responsible for
 * any conversion from source to target types.
 * 
 * @author Graeme Rocher
 * @since 1.0
 */
public class EntityAccess {

    private static final Set EXCLUDED_PROPERTIES = new HashSet() {{
        add("class"); add("metaClass");
    }};

    protected Object entity;
    private BeanWrapper beanWrapper;
    private PersistentEntity persistentEntity;

    public EntityAccess(PersistentEntity persistentEntity, Object entity) {
        this.entity = entity;
        this.persistentEntity = persistentEntity;
        this.beanWrapper = new BeanWrapperImpl(entity);
    }

    public Object getEntity() {
        return entity;
    }

    public void setConversionService(ConversionService conversionService) {
        this.beanWrapper.setConversionService(conversionService);
    }

    public Object getProperty(String name) {
        return beanWrapper.getPropertyValue(name);
    }

    public void setProperty(String name, Object value) {
        beanWrapper.setPropertyValue(name, value);
    }

    public Object getIdentifier() {
        String idName = getIdentifierName(persistentEntity.getMapping());
        return getProperty(idName);

    }

    public void setIdentifier(Object id) {
        String idName = getIdentifierName(persistentEntity.getMapping());
        setProperty(idName, id);
    }

    protected String getIdentifierName(ClassMapping cm) {
        return cm.getIdentifier().getIdentifierName()[0];
    }

    public String getIdentifierName() {
        return getIdentifierName(persistentEntity.getMapping());
        
    }

    public void setPropertyNoConversion(String name, Object value) {
        final PropertyDescriptor pd = beanWrapper.getPropertyDescriptor(name);
        if(pd != null) {
            final Method writeMethod = pd.getWriteMethod();
            if(writeMethod!=null) {
                ReflectionUtils.invokeMethod(writeMethod, beanWrapper.getWrappedInstance(), value);
            }
        }
    }

    /**
     * Refreshes the object from entity state
     */
    public void refresh() {
        final PropertyDescriptor[] descriptors = beanWrapper.getPropertyDescriptors();
        for (PropertyDescriptor descriptor : descriptors) {
            final String name = descriptor.getName();
            if(!EXCLUDED_PROPERTIES.contains(name)) {
                if(beanWrapper.isReadableProperty(name) && beanWrapper.isWritableProperty(name)) {
                    Object newValue = getProperty(name);
                    setProperty(name, newValue);
                }

            }
        }
    }
}
