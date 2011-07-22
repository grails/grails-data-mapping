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

package org.grails.datastore.mapping.jpa.config;

import java.lang.reflect.Field;

import javax.persistence.Column;
import javax.persistence.Table;

import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;

/**
 * MappingFactory for JPA that maps entities to {@link Table} instances and properties to {@link Column} instances
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class JpaMappingFactory extends MappingFactory<Table, Column> {

    @Override
    public Table createMappedForm(PersistentEntity entity) {
        return (Table)entity.getJavaClass().getAnnotation(Table.class);
    }

    @Override
    public Column createMappedForm(PersistentProperty mpp) {
        Field field;
        try {
            field = mpp.getOwner().getJavaClass().getDeclaredField(mpp.getName());
            return field.getAnnotation(Column.class);
        } catch (SecurityException e) {
            return null;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
