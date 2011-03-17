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

package org.springframework.datastore.mapping.document.config;

import org.springframework.datastore.mapping.model.AbstractClassMapping;
import org.springframework.datastore.mapping.model.AbstractPersistentEntity;
import org.springframework.datastore.mapping.model.ClassMapping;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;

public class DocumentPersistentEntity extends AbstractPersistentEntity<Collection> {

    public DocumentPersistentEntity(@SuppressWarnings("rawtypes") Class javaClass, MappingContext context) {
        super(javaClass, context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ClassMapping<Collection> getMapping() {
        return new DocumentCollectionMapping(this, context);
    }

    public class DocumentCollectionMapping extends AbstractClassMapping<Collection> {
        public DocumentCollectionMapping(PersistentEntity entity, MappingContext context) {
            super(entity, context);
        }
        @Override
        public Collection getMappedForm() {
            return (Collection) context.getMappingFactory().createMappedForm(DocumentPersistentEntity.this);
        }
    }
}
