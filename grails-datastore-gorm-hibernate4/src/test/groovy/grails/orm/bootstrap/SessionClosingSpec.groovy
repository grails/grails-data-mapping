package grails.orm.bootstrap

import grails.gorm.DetachedCriteria
import grails.orm.bootstrap.HibernateDatastoreSpringInitializer
import grails.persistence.Entity
import org.apache.tomcat.jdbc.pool.DataSource
import org.apache.tomcat.jdbc.pool.PoolProperties
import org.hibernate.HibernateException
import org.springframework.context.support.GenericApplicationContext
import org.springframework.jdbc.datasource.DriverManagerDataSource
import spock.lang.Issue
import spock.lang.Specification

/*
 * Copyright 2014 original authors
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

/**
 * @author graemerocher
 */
@Issue('GRAILS-11745')
class SessionClosingSpec extends Specification {

    void "Test that connections do not leak when no session is prebound"() {
        given: "An initializer instance"
            def datastoreInitializer = new HibernateDatastoreSpringInitializer(Person)
            def applicationContext = new GenericApplicationContext()
            def pool = new PoolProperties()
            pool.driverClassName = org.h2.Driver.name
            pool.url = "jdbc:h2:mem:grailsDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1"
            pool.username = "sa"
            pool.password = ""
            def dataSource = new DataSource(pool)
            applicationContext.beanFactory.registerSingleton("dataSource", dataSource)

        when:"The application context is configured and a GORM method is executed"
            datastoreInitializer.configureForBeanDefinitionRegistry(applicationContext)
            applicationContext.refresh()
            new DetachedCriteria<Person>(Person).count()

        then:"All connections were closed"
            thrown HibernateException

        when:"The application context is configured and a GORM method is executed"
            Person.withNewSession {
                new DetachedCriteria<Person>(Person).count()
            }


        then:"All connections were closed"
            dataSource.numActive == 0
            dataSource.numIdle == 10


    }
}

