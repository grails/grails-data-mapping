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
package org.grails.orm.hibernate.cfg;

import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.orm.hibernate.persister.entity.GroovyAwareJoinedSubclassEntityPersister;
import org.grails.orm.hibernate.persister.entity.GroovyAwareSingleTableEntityPersister;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.Table;

/**
 * Handles the binding Grails domain classes and properties to the Hibernate runtime meta model.
 * Based on the HbmBinder code in Hibernate core and influenced by AnnotationsBinder.
 *
 * @author Graeme Rocher
 * @since 0.1
 */
public class GrailsDomainBinder extends AbstractGrailsDomainBinder {

    protected Class<?> getGroovyAwareJoinedSubclassEntityPersisterClass() {
        return GroovyAwareJoinedSubclassEntityPersister.class;
    }

    @Override
    protected void handleLazyProxy(PersistentEntity domainClass, PersistentProperty grailsProperty) {
        HibernateUtils.handleLazyProxy(domainClass, grailsProperty);
    }

    protected Class<?> getGroovyAwareSingleTableEntityPersisterClass() {
        return GroovyAwareSingleTableEntityPersister.class;
    }

    protected void handleUniqueConstraint(PersistentProperty property, Column column, String path, Table table, String columnName, String sessionFactoryBeanName) {
        final PropertyConfig mappedForm = (PropertyConfig) property.getMapping().getMappedForm();
        if (mappedForm.isUnique()) {
            if (!mappedForm.isUniqueWithinGroup()) {
                column.setUnique(true);
            }
            else {
                createKeyForProps(property, path, table, columnName, mappedForm.getUniquenessGroup(), sessionFactoryBeanName);
            }
        }

    }

    protected boolean identityEnumTypeSupports(Class<?> propertyType) {
        return IdentityEnumType.supports(propertyType);
    }

    protected boolean isNotEmpty(String s) {
        return GrailsHibernateUtil.isNotEmpty(s);
    }

    protected String qualify(String prefix, String name) {
        return GrailsHibernateUtil.qualify(prefix, name);
    }

    protected String unqualify(String qualifiedName) {
        return GrailsHibernateUtil.unqualify(qualifiedName);
    }

    @Override
    protected void bindOneToOneInternal(org.grails.datastore.mapping.model.types.OneToOne property, OneToOne oneToOne, String path) {
        oneToOne.setReferenceToPrimaryKey(false);
    }
}
