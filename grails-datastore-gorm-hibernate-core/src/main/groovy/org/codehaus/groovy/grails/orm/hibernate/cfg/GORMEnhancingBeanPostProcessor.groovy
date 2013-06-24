/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate.cfg

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin
import org.codehaus.groovy.grails.plugins.support.BeanPostProcessorAdapter
import org.hibernate.EntityMode
import org.hibernate.SessionFactory
import org.hibernate.metadata.ClassMetadata
import org.springframework.beans.BeanInstantiationException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Enhances an existing SessionFactory with GORM behavior.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
class GORMEnhancingBeanPostProcessor extends BeanPostProcessorAdapter implements ApplicationContextAware {

    ApplicationContext applicationContext

    private static Class sessionFactoryBeanClass
    static {
        sessionFactoryBeanClass = Class.forName(
            "org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean",
            true, Thread.currentThread().contextClassLoader)
    }

    Object postProcessAfterInitialization(bean, String beanName) {
        if (bean instanceof SessionFactory) {
            GrailsApplication application
            if (applicationContext.containsBean(GrailsApplication.APPLICATION_ID)) {
                application = applicationContext.getBean(GrailsApplication.APPLICATION_ID)
            }
            else {
                application = new DefaultGrailsApplication()
                application.initialise()
            }

            bean.allClassMetadata.each { className, ClassMetadata metadata ->
                Class mappedClass = metadata.getMappedClass(EntityMode.POJO)

                if (!application.getDomainClass(mappedClass.name)) {
                    application.addDomainClass(mappedClass)
                }
            }

            try {
                DomainClassGrailsPlugin.enhanceDomainClasses(application, applicationContext)
                HibernateUtilities.enhanceSessionFactory(bean, application, applicationContext)
            }
            catch (Throwable e) {
                throw new BeanInstantiationException(sessionFactoryBeanClass,
                        "Error configuring GORM dynamic behavior: $e.message", e)
            }
        }
        return bean
    }
}
