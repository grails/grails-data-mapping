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

package org.grails.datastore.mapping.document.config;

import org.grails.datastore.mapping.model.*;

public class DocumentPersistentEntity extends AbstractPersistentEntity<Collection> {

    private DocumentCollectionMapping classMapping;

    public DocumentPersistentEntity(@SuppressWarnings("rawtypes") Class javaClass, MappingContext context) {
        super(javaClass, context);
        this.classMapping = new DocumentCollectionMapping(this, context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ClassMapping<Collection> getMapping() {
        return classMapping;
    }

    public class DocumentCollectionMapping extends AbstractClassMapping<Collection> {
        private Collection mappedForm;

        private IdentityMapping identityMapping;
        public DocumentCollectionMapping(PersistentEntity entity, MappingContext context) {
            super(entity, context);
            this.mappedForm = (Collection) context.getMappingFactory().createMappedForm(DocumentPersistentEntity.this);
        }
        @Override
        public Collection getMappedForm() {
            return mappedForm ;
        }

        @Override
        public IdentityMapping getIdentifier() {
            if(identityMapping == null) {
                identityMapping = context.getMappingFactory().createIdentityMapping(this);
            }
            return identityMapping;
        }
    }
}
