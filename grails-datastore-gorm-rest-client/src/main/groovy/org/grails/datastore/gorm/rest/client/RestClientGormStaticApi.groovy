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
package org.grails.datastore.gorm.rest.client

import grails.plugins.rest.client.async.AsyncRestBuilder
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.rest.client.RestClientDatastore
import org.springframework.transaction.PlatformTransactionManager

/**
 * Extensions to the static API for REST
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class RestClientGormStaticApi<D> extends GormStaticApi<D> {
    RestClientGormStaticApi(Class<D> persistentClass, RestClientDatastore datastore, List<FinderMethod> finders) {
        super(persistentClass, datastore, finders)
    }

    RestClientGormStaticApi(Class<D> persistentClass, RestClientDatastore datastore, List<FinderMethod> finders, PlatformTransactionManager transactionManager) {
        super(persistentClass, datastore, finders, transactionManager)
    }

    /**
     * Obtains the RestBuilder instance used by this domain
     */
    AsyncRestBuilder getRestBuilder() {
        ((RestClientDatastore)datastore).asyncRestClients.get(persistentEntity)
    }
}
