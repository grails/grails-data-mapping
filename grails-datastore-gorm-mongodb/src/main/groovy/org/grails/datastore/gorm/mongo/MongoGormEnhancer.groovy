/* Copyright (C) 2010 SpringSource
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
package org.grails.datastore.gorm.mongo

import groovy.transform.CompileDynamic
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.mapping.core.Datastore
import org.springframework.transaction.PlatformTransactionManager
/**
 * GORM enhancer for Mongo.
 *
 * @author Graeme Rocher
 */
class MongoGormEnhancer extends GormEnhancer {

    MongoGormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)


        DynamicFinder.registerNewMethodExpression(NearSphere)
        DynamicFinder.registerNewMethodExpression(Near)
        DynamicFinder.registerNewMethodExpression(WithinBox)
        DynamicFinder.registerNewMethodExpression(WithinPolygon)
        DynamicFinder.registerNewMethodExpression(WithinCircle)
        DynamicFinder.registerNewMethodExpression(GeoWithin)
        DynamicFinder.registerNewMethodExpression(GeoIntersects)
    }

    @Override
    @CompileDynamic
    protected void registerConstraints(Datastore datastore) {
        super.registerConstraints(datastore)
//        ConstrainedProperty.registerNewConstraint("unique", new UniqueConstraintFactory(datastore));
    }

    MongoGormEnhancer(Datastore datastore) {
        this(datastore, null)
    }

    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        return new MongoGormStaticApi<D>(cls, datastore, getFinders(), transactionManager)
    }

    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        final api = new MongoGormInstanceApi<D>(cls, datastore)
        api.failOnError = failOnError
        return api
    }
}
