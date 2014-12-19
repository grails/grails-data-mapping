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

import java.util.Arrays;
import java.util.List;

import grails.core.GrailsDomainClass;
import grails.core.GrailsDomainClassProperty;
import grails.validation.ConstrainedProperty;
import grails.validation.Constraint;
import org.grails.orm.hibernate.persister.entity.GroovyAwareJoinedSubclassEntityPersister;
import org.grails.orm.hibernate.persister.entity.GroovyAwareSingleTableEntityPersister;
import org.grails.orm.hibernate.validation.UniqueConstraint;
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

    protected Class<?> getGroovyAwareSingleTableEntityPersisterClass() {
        return GroovyAwareSingleTableEntityPersister.class;
    }

    protected void handleLazyProxy(GrailsDomainClass domainClass, GrailsDomainClassProperty grailsProperty) {
        HibernateUtils.handleLazyProxy(domainClass, grailsProperty);
    }

    protected void handleUniqueConstraint(GrailsDomainClassProperty property, Column column, String path, Table table, String columnName, String sessionFactoryBeanName) {
        ConstrainedProperty cp = getConstrainedProperty(property);
        if (cp != null && cp.hasAppliedConstraint(UniqueConstraint.UNIQUE_CONSTRAINT)) {
            Constraint appliedConstraint = cp.getAppliedConstraint(UniqueConstraint.UNIQUE_CONSTRAINT);
            if (appliedConstraint instanceof UniqueConstraint) {
                UniqueConstraint uc = (UniqueConstraint) appliedConstraint;
                if (uc != null && uc.isUnique()) {
                    if (!uc.isUniqueWithinGroup()) {
                        column.setUnique(true);
                    }
                    else if (uc.getUniquenessGroup().size() > 0) {
                        createKeyForProps(property, path, table, columnName, uc.getUniquenessGroup(), sessionFactoryBeanName);
                    }
                }
            }
        }
        else {
            Object val = cp != null ? cp.getMetaConstraintValue(UniqueConstraint.UNIQUE_CONSTRAINT) : null;
            if (val instanceof Boolean) {
                column.setUnique((Boolean)val);
            }
            else if (val instanceof String) {
                createKeyForProps(property, path, table, columnName, Arrays.asList((String) val), sessionFactoryBeanName);
            }
            else if (val instanceof List<?> && ((List<?>)val).size() > 0) {
                createKeyForProps(property, path, table, columnName, (List<?>)val, sessionFactoryBeanName);
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
    protected void bindOneToOneInternal(GrailsDomainClassProperty property, OneToOne oneToOne, String path) {
        oneToOne.setReferenceToPrimaryKey(false);
    }
}
