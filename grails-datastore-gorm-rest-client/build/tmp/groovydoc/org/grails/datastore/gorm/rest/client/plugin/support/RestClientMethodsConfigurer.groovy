/* Copyright (C) 2013 original authors
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
package org.grails.datastore.gorm.rest.client.plugin.support

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.rest.client.RestClientGormEnhancer
import org.grails.datastore.gorm.rest.client.RestClientGormStaticApi
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.rest.client.RestClientDatastore
import org.springframework.transaction.PlatformTransactionManager
import org.grails.datastore.gorm.plugin.support.DynamicMethodsConfigurer

/**
 * Methods configurer for the GORM REST client
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class RestClientMethodsConfigurer extends DynamicMethodsConfigurer {
    RestClientMethodsConfigurer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    @Override
    protected GormStaticApi createGormStaticApi(Class cls, List<FinderMethod> finders) {
        return new RestClientGormStaticApi(cls, (RestClientDatastore)datastore, finders, transactionManager)
    }

    @Override
    protected GormEnhancer createEnhancer() {
        return new RestClientGormEnhancer((RestClientDatastore)datastore, transactionManager)
    }

    @Override
    String getDatastoreType() {
        "RestClient"
    }
}
