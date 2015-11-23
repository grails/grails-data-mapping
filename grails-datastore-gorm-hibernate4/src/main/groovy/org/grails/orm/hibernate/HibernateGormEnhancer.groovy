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

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.GormValidationApi
import org.grails.datastore.mapping.config.Entity
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.grails.orm.hibernate.cfg.Mapping
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

    HibernateGormEnhancer(HibernateDatastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    @Override
    protected boolean appliesToDatastore(Datastore datastore, PersistentEntity entity) {
        if(GrailsHibernateUtil.usesDatasource(entity, ((AbstractHibernateDatastore)datastore).getDataSourceName())) {
            return super.appliesToDatastore(datastore, entity)
        }
        return false;
    }

    @Override
    Set<String> allQualifiers(Datastore datastore, PersistentEntity entity) {
        def dataSourceName = GrailsHibernateUtil.getDefaultDataSource(entity)
        def datastoreStoreDataSourceName = ((HibernateDatastore) datastore).dataSourceName
        Set<String> qualifiers = []

        def allMappedDataSources = GrailsHibernateUtil.getDatasourceNames(entity)
        if(datastoreStoreDataSourceName.equals(dataSourceName) ) {
            qualifiers.add(Entity.DEFAULT_DATA_SOURCE)
        }
        if(allMappedDataSources.contains(datastoreStoreDataSourceName)) {
            qualifiers.add(datastoreStoreDataSourceName)
        }
        return qualifiers

    }


    protected <D> GormValidationApi<D> getValidationApi(Class<D> cls) {
        new HibernateGormValidationApi<D>(cls, (HibernateDatastore)datastore, Thread.currentThread().contextClassLoader)
    }

    @Override
    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        new HibernateGormStaticApi<D>(cls, (HibernateDatastore)datastore, getFinders(), Thread.currentThread().contextClassLoader, transactionManager)
    }

    @Override
    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        new HibernateGormInstanceApi<D>(cls, (HibernateDatastore)datastore, Thread.currentThread().contextClassLoader)
    }

    @Override
    protected void registerConstraints(Datastore datastore) {
        // no-op
    }
}
