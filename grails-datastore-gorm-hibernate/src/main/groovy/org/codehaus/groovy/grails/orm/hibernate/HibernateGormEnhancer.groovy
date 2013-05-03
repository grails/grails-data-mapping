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

package org.codehaus.groovy.grails.orm.hibernate

import groovy.transform.CompileStatic;

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.orm.hibernate.cfg.HibernateNamedQueriesBuilder
import org.codehaus.groovy.grails.orm.hibernate.metaclass.CountByPersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.FindAllByBooleanPropertyPersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.FindAllByPersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.FindByBooleanPropertyPersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.FindByPersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.FindOrCreateByPersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.FindOrSaveByPersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.ListOrderByPersistentMethod
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.GormValidationApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.PersistentEntity
import org.springframework.transaction.PlatformTransactionManager

/**
 * Extended GORM Enhancer that fills out the remaining GORM for Hibernate methods
 * and implements string-based query support via HQL.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class HibernateGormEnhancer extends GormEnhancer {

    private ClassLoader classLoader
    private GrailsApplication grailsApplication

    HibernateGormEnhancer(HibernateDatastore datastore,
            PlatformTransactionManager transactionManager,
            GrailsApplication grailsApplication) {
        super(datastore, transactionManager)
        this.grailsApplication = grailsApplication
        classLoader = grailsApplication.classLoader
        finders = createPersistentMethods(grailsApplication, classLoader, datastore)
    }

    @Override
    protected void registerConstraints(Datastore datastore) {
        // no-op
    }

    static List createPersistentMethods(GrailsApplication grailsApplication, ClassLoader classLoader, Datastore datastore) {
        HibernateDatastore hibernateDatastore = (HibernateDatastore)datastore
        def sessionFactory = hibernateDatastore.sessionFactory
        Collections.unmodifiableList([
            new FindAllByPersistentMethod(hibernateDatastore, grailsApplication, sessionFactory, classLoader),
            new FindAllByBooleanPropertyPersistentMethod(hibernateDatastore, grailsApplication, sessionFactory, classLoader),
            new FindOrCreateByPersistentMethod(hibernateDatastore, grailsApplication, sessionFactory, classLoader),
            new FindOrSaveByPersistentMethod(hibernateDatastore, grailsApplication, sessionFactory, classLoader),
            new FindByPersistentMethod(hibernateDatastore, grailsApplication, sessionFactory, classLoader),
            new FindByBooleanPropertyPersistentMethod(hibernateDatastore, grailsApplication, sessionFactory, classLoader),
            new CountByPersistentMethod(hibernateDatastore, grailsApplication, sessionFactory, classLoader),
            new ListOrderByPersistentMethod(hibernateDatastore, grailsApplication, sessionFactory, classLoader) ])
    }

    protected <D> GormValidationApi<D> getValidationApi(Class<D> cls) {
        new HibernateGormValidationApi<D>(cls, (HibernateDatastore)datastore, classLoader)
    }

    @Override
    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        new HibernateGormStaticApi<D>(cls, (HibernateDatastore)datastore, finders, classLoader, transactionManager)
    }

    @Override
    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        new HibernateGormInstanceApi<D>(cls, (HibernateDatastore)datastore, classLoader)
    }

    @Override
    protected void registerNamedQueries(PersistentEntity entity, namedQueries) {
        if (grailsApplication == null) {
            return
        }

        def domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, entity.name)
        if (domainClass) {
            new HibernateNamedQueriesBuilder(domainClass, finders).evaluate((Closure)namedQueries)
        }
    }
}
