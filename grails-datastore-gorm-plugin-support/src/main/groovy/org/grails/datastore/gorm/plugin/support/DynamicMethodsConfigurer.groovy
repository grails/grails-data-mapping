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
package org.grails.datastore.gorm.plugin.support

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.utils.InstanceProxy
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.config.GormProperties
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.springframework.transaction.PlatformTransactionManager

/**
 * Utility class for use by plugins in configuration of dynamic methods.
 * Subclasses should provide the implementation of getDatastoreType and
 * override protected methods to configure behavior.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
abstract class DynamicMethodsConfigurer {

    Datastore datastore
    PlatformTransactionManager transactionManager

    DynamicMethodsConfigurer(Datastore datastore, PlatformTransactionManager transactionManager) {
        this.datastore = datastore
        this.transactionManager = transactionManager
    }

    abstract String getDatastoreType()

    boolean hasExistingDatastore = false
    boolean failOnError = false

    void configure() {
        def enhancer = createEnhancer()

        String type = getDatastoreType()
        String typeLower = type.toLowerCase()

        for (entity in datastore.mappingContext.persistentEntities) {
            def cls = entity.javaClass
            def cpf = ClassPropertyFetcher.forClass(cls)
            def mappedWith = cpf.getStaticPropertyValue(GormProperties.MAPPING_STRATEGY, String)
            if (hasExistingDatastore) {
                if (mappedWith == typeLower) {
                    enhancer.enhance(entity)
                }
                else {
                    def staticApi = createGormStaticApi(cls, enhancer.getFinders())
                    def instanceApi = createGormInstanceApi(cls)
                    cls.metaClass.static."get$type" = {-> staticApi }
                    cls.metaClass."get$type" = {-> new InstanceProxy(instance:delegate, target:instanceApi) }
                }
            }
            else {
                if (mappedWith == typeLower || mappedWith == null) {
                    enhancer.enhance(entity)
                }
            }
        }
    }

    protected GormStaticApi createGormStaticApi(Class cls, List<FinderMethod> finders) {
        return new GormStaticApi(cls,datastore, finders)
    }

    protected GormInstanceApi createGormInstanceApi(Class cls) {
        def instanceApi = new GormInstanceApi(cls, datastore)
        instanceApi.failOnError = failOnError
        return instanceApi
    }

    protected GormEnhancer createEnhancer() {
        def enhancer = new GormEnhancer(datastore, transactionManager)
        enhancer.failOnError = failOnError
        return enhancer
    }
}
