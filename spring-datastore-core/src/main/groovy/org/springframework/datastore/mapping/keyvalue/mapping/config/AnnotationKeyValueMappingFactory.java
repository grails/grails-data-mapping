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
package org.springframework.datastore.mapping.keyvalue.mapping.config;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.datastore.mapping.annotation.Index;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.reflect.ClassPropertyFetcher;
import org.springframework.util.ReflectionUtils;

/**
 * Uses annotations to configure entity mappies
 * 
 * @author Graeme Rocher
 * @since 1.1
 */
public class AnnotationKeyValueMappingFactory extends KeyValueMappingFactory{
    public AnnotationKeyValueMappingFactory(String keyspace) {
        super(keyspace);
    }



    @Override
    public KeyValue createMappedForm(PersistentProperty mpp) {
        final Class javaClass = mpp.getOwner().getJavaClass();
        final ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(javaClass);

        final PropertyDescriptor pd = cpf.getPropertyDescriptor(mpp.getName());
        final KeyValue kv = super.createMappedForm(mpp);
        Index index = AnnotationUtils.getAnnotation(pd.getReadMethod(), Index.class);

        if(index == null) {
            final Field field = ReflectionUtils.findField(javaClass, mpp.getName());
            if(field != null) {
                ReflectionUtils.makeAccessible(field);
                index = field.getAnnotation(Index.class);
            }
        }
        if(index != null) {
            kv.setIndex(true);
        }

        return kv;

    }
}
