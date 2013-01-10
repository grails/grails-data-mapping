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
package org.grails.datastore.gorm.jpa.bean.factory

import javax.persistence.EntityManagerFactory

import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.mapping.jpa.JpaDatastore
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.beans.factory.FactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.orm.jpa.JpaTransactionManager

class JpaDatastoreFactoryBean implements FactoryBean<JpaDatastore>, ApplicationContextAware {

    MappingContext mappingContext
    ApplicationContext applicationContext

    JpaDatastore getObject() {
        JpaTransactionManager transactionManager = applicationContext.getBean(JpaTransactionManager)
        EntityManagerFactory entityManagerFactory = applicationContext.getBean(EntityManagerFactory)
        def datastore = new JpaDatastore(mappingContext, entityManagerFactory, transactionManager, (ConfigurableApplicationContext)applicationContext)
        applicationContext.addApplicationListener new DomainEventListener(datastore)
        applicationContext.addApplicationListener new AutoTimestampEventListener(datastore)
        datastore
    }

    Class<?> getObjectType() { JpaDatastore }

    boolean isSingleton() { true }
}
