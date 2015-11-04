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
package org.grails.orm.hibernate

import grails.core.GrailsApplication
import grails.core.GrailsDomainClass
import grails.util.GrailsClassUtils
import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.orm.hibernate.cfg.HibernateNamedQueriesBuilder
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.core.support.GrailsDomainConfigurationUtil
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.mapping.proxy.ProxyFactory
import org.springframework.beans.BeanUtils
import org.springframework.beans.MutablePropertyValues
import org.springframework.context.ApplicationContext
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.validation.DataBinder

@CompileStatic
abstract class AbstractHibernateGormEnhancer extends GormEnhancer {

    protected ClassLoader classLoader
    protected GrailsApplication grailsApplication

    protected AbstractHibernateGormEnhancer(AbstractHibernateDatastore datastore,
            PlatformTransactionManager transactionManager,
            GrailsApplication grailsApplication) {
        super(datastore, transactionManager)
        this.grailsApplication = grailsApplication
        classLoader = grailsApplication.classLoader
        getFinders()
    }

    @Override
    @CompileStatic
    void enhance(PersistentEntity e, boolean onlyExtendedMethods) {
        super.enhance(e, onlyExtendedMethods)

        GrailsDomainClass domainClass = (GrailsDomainClass)grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, e.name)
        datastore.mappingContext.addEntityValidator(e, domainClass.validator)
    }

    @Override
    protected void registerConstraints(Datastore datastore) {
        // no-op
    }

    @Override
    @CompileStatic
    protected void registerNamedQueries(PersistentEntity entity, Closure namedQueries) {
        if (grailsApplication == null) {
            return
        }
        def domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, entity.name)
        if (domainClass) {
            new HibernateNamedQueriesBuilder(domainClass, entity, getFinders()).evaluate(namedQueries)
        }
    }

}
