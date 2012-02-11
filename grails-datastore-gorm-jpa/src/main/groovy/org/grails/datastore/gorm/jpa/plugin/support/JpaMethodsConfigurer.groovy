/* Copyright (C) 2012 SpringSource
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
package org.grails.datastore.gorm.jpa.plugin.support

import org.grails.datastore.gorm.plugin.support.DynamicMethodsConfigurer
import org.grails.datastore.mapping.core.Datastore
import org.springframework.transaction.PlatformTransactionManager
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.jpa.JpaGormEnhancer
import org.grails.datastore.gorm.jpa.JpaInstanceApi
import org.grails.datastore.gorm.jpa.JpaStaticApi

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class JpaMethodsConfigurer extends DynamicMethodsConfigurer{

    JpaMethodsConfigurer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    @Override
    String getDatastoreType() {
        return "jpa"
    }

    @Override
    protected GormStaticApi createGormStaticApi(Class cls, List<FinderMethod> finders) {
        return new JpaStaticApi(cls, datastore,finders)
    }

    @Override
    protected GormInstanceApi createGormInstanceApi(Class cls) {
        return new JpaInstanceApi(cls, datastore)
    }

    @Override
    protected GormEnhancer createEnhancer() {
        return new JpaGormEnhancer(datastore,transactionManager)
    }


}
