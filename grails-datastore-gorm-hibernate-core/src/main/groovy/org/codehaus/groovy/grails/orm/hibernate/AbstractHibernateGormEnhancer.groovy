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

import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.orm.hibernate.cfg.HibernateNamedQueriesBuilder
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.GormValidationApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.PersistentEntity
import org.springframework.transaction.PlatformTransactionManager

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
    protected void registerConstraints(Datastore datastore) {
        // no-op
    }

    @Override
    protected void registerNamedQueries(PersistentEntity entity, namedQueries) {
        if (grailsApplication == null) {
            return
        }

        def domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, entity.name)
        if (domainClass) {
            new HibernateNamedQueriesBuilder(domainClass, getFinders(), entity.mappingContext.conversionService).evaluate((Closure)namedQueries)
        }
    }
}
