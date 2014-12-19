/*
 * Copyright 2004-2005 the original author or authors.
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
package org.grails.orm.hibernate;

import java.util.Collection;

import grails.core.GrailsApplication;
import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;

/**
 * An implementation of the GrailsDomainClass interface that allows Classes
 * mapped in Hibernate to integrate with Grails' validation, dynamic methods
 * etc. seamlessly.
 *
 * @author Graeme Rocher
 * @since 0.1
 */
@SuppressWarnings("unchecked")
public class GrailsHibernateDomainClass extends AbstractGrailsHibernateDomainClass {

    /**
     * Contructor to be used by all child classes to create a new instance
     * and get the name right.
     *
     * @param clazz          the Grails class
     * @param sessionFactory The Hibernate SessionFactory instance
     * @param sessionFactoryName
     * @param application
     * @param metaData       The ClassMetaData for this class retrieved from the SF
     */
    public GrailsHibernateDomainClass(Class<?> clazz, SessionFactory sessionFactory, String sessionFactoryName,
            GrailsApplication application, ClassMetadata metaData) {
        super(clazz, sessionFactory, sessionFactoryName, application, metaData);
    }

    protected void setRelatedClassType(GrailsHibernateDomainClassProperty prop, AssociationType assType, Type hibernateType) {
        try {
            String associatedEntity = assType.getAssociatedEntityName((SessionFactoryImplementor) getSessionFactory());
            ClassMetadata associatedMetaData = getSessionFactory().getClassMetadata(associatedEntity);
            prop.setRelatedClassType(associatedMetaData.getMappedClass(EntityMode.POJO));
        }
        catch (MappingException me) {
            // other side must be a value object
            if (hibernateType.isCollectionType()) {
                prop.setRelatedClassType(Collection.class);
            }
        }
    }
}
