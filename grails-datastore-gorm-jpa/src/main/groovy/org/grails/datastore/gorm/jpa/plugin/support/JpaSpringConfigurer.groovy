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

import org.grails.datastore.gorm.jpa.bean.factory.JpaDatastoreFactoryBean
import org.grails.datastore.gorm.jpa.bean.factory.JpaMappingContextFactoryBean
import org.grails.datastore.gorm.jpa.support.JpaPersistenceContextInterceptor
import org.grails.datastore.gorm.plugin.support.SpringConfigurer
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor

/**
 * Configures JPA. Assumes an entityManagerFactory bean has been configured by the application.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class JpaSpringConfigurer extends SpringConfigurer {

    @Override
    String getDatastoreType() { "jpa" }

    @Override
    protected Closure configureSpring(Closure customizer) {

        return {
            jpaMappingContext(JpaMappingContextFactoryBean)
            jpaDatastore(JpaDatastoreFactoryBean) {
                mappingContext = jpaMappingContext
            }
            jpaPersistenceInterceptor(JpaPersistenceContextInterceptor, ref("jpaDatastore"))

            if (manager?.hasGrailsPlugin("controllers")) {
                String interceptorName = "jpaOpenSessionInViewInterceptor"
                "${interceptorName}"(OpenEntityManagerInViewInterceptor) { bean ->
                    bean.autowire = true
                }
                if (getSpringConfig().containsBean("controllerHandlerMappings")) {
                    controllerHandlerMappings.interceptors << ref(interceptorName)
                }
                if (getSpringConfig().containsBean("annotationHandlerMapping")) {
                    if (annotationHandlerMapping.interceptors) {
                        annotationHandlerMapping.interceptors << ref(interceptorName)
                    }
                    else {
                        annotationHandlerMapping.interceptors = [ref(interceptorName)]
                    }
                }
            }
        }
    }

    @Override
    Closure getSpringCustomizer() {
        return null  // do nothing, handled by configureSpring
    }
}
