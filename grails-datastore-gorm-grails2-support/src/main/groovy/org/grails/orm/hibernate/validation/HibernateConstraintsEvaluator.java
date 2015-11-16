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
package org.grails.orm.hibernate.validation;

import java.util.Map;

import grails.validation.Constrained;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.orm.hibernate.cfg.PropertyConfig;

/**
 * Extends default implementation to add Hibernate specific exceptions.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class HibernateConstraintsEvaluator extends DefaultConstraintEvaluator {

    private MappingContext mappingContext;

    public HibernateConstraintsEvaluator(Map<String, Object> defaultConstraints) {
        super(defaultConstraints);
    }

    public HibernateConstraintsEvaluator() {
        // default
    }

    public void setMappingContext(MappingContext mappingContext) {
        this.mappingContext = mappingContext;
    }

    @Override
    protected boolean canPropertyBeConstrained(GrailsDomainClassProperty property) {
        PersistentEntity entity = getPersistentEntity(property);
        PersistentProperty p = getPersistentProperty(property);

        if(p != null) {
            PropertyConfig propertyConfig = getPropertyConfig(p);

            if(propertyConfig != null) {
                property.setDerived( propertyConfig.getFormula() != null);
            }

            PersistentProperty identity = entity.getIdentity();
            if(identity != null && identity.getName().equals(property.getName())) {
                return false;
            }
        }

        return super.canPropertyBeConstrained(property);
    }

    @Override
    protected boolean canApplyNullableConstraint(String propertyName, GrailsDomainClassProperty property, Constrained constrained) {
        PropertyConfig propertyConfig = getPropertyConfig(property);
        if(propertyConfig != null) {
            property.setDerived( propertyConfig.getFormula() != null);
        }
        return super.canApplyNullableConstraint(propertyName, property, constrained);
    }

    @Override
    protected void applyDefaultNullableConstraint(GrailsDomainClassProperty p, Constrained cp) {


        PropertyConfig propertyConfig = getPropertyConfig(p);

        if(propertyConfig != null) {
            if(propertyConfig.getFormula() != null) {
                p.setDerived(true);
            }
        }
        boolean insertable = propertyConfig == null || propertyConfig.isInsertable();

        if (!insertable) {
            cp.applyConstraint(ConstrainedProperty.NULLABLE_CONSTRAINT,true);
        }
        else {
            if(canApplyNullableConstraint(p.getName(), p, cp)) {
                super.applyDefaultNullableConstraint(p, cp);
            }
        }
    }

    private PropertyConfig getPropertyConfig(GrailsDomainClassProperty p) {
        PersistentProperty property = getPersistentProperty(p);
        return getPropertyConfig(property);
    }

    private PropertyConfig getPropertyConfig(PersistentProperty property) {
        PropertyConfig propertyConfig = null;
        if(property != null) {
            propertyConfig = (PropertyConfig) property
                    .getMapping()
                    .getMappedForm();
        }
        return propertyConfig;
    }

    protected PersistentProperty getPersistentProperty(GrailsDomainClassProperty p ) {
        PersistentProperty property = null;
        PersistentEntity entity = getPersistentEntity(p);
        if(entity != null) {
            property = entity
                    .getPropertyByName(p.getName());
        }
        return property;
    }

    private PersistentEntity getPersistentEntity(GrailsDomainClassProperty p) {
        final GrailsDomainClass domainClass = p.getDomainClass();
        PersistentEntity entity = null;
        if(mappingContext != null) {

            entity = mappingContext.getPersistentEntity(domainClass.getFullName());
        }
        return entity;
    }
}
