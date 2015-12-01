/*
 * Copyright 2015 original authors
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

package grails.cassandra.bootstrap

import groovy.transform.InheritConstructors
import org.grails.datastore.gorm.bootstrap.AbstractDatastoreInitializer
import org.grails.datastore.gorm.cassandra.CassandraGormEnhancer
import org.grails.datastore.gorm.cassandra.bean.factory.CassandraDatastoreFactoryBean
import org.grails.datastore.gorm.cassandra.bean.factory.CassandraMappingContextFactoryBean
import org.grails.datastore.gorm.cassandra.bean.factory.DefaultMappingHolder
import org.grails.datastore.gorm.support.AbstractDatastorePersistenceContextInterceptor
import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.springframework.beans.factory.support.BeanDefinitionRegistry


/**
 * @author Graeme Rocher
 * @since 4.0
 */
@InheritConstructors
class CassandraDatastoreSpringInitializer extends AbstractDatastoreInitializer {

    boolean developmentMode = false
    String defaultKeyspaceName = ''

    @Override
    protected Class<AbstractDatastorePersistenceContextInterceptor> getPersistenceInterceptorClass() {
        DatastorePersistenceContextInterceptor
    }

    @Override
    Closure getBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry) {
        {->
            def config = configuration
            def keyspaceName = config.getProperty(CassandraDatastore.KEYSPACE_NAME, defaultKeyspaceName)
            def defaultMapping = config.getProperty(CassandraDatastore.DEFAULT_MAPPING, Closure, null)

            def callable = getCommonConfiguration(beanDefinitionRegistry, "cassandra")
            callable.delegate = delegate
            callable.call()

            cassandraMappingContext(CassandraMappingContextFactoryBean) {
                keyspace = keyspaceName
                grailsApplication = ref('grailsApplication')
                if (defaultMapping) {
                    delegate.defaultMapping = new DefaultMappingHolder(defaultMapping)
                }
            }
            cassandraDatastore(CassandraDatastoreFactoryBean) {
                mappingContext = cassandraMappingContext
                delegate.config = config
                delegate.developmentMode = developmentMode
            }

            cassandraTemplate(cassandraDatastore : "getCassandraTemplate")

            callable = getAdditionalBeansConfiguration(beanDefinitionRegistry, "cassandra")
            callable.delegate = delegate
            callable.call()

            "org.grails.gorm.cassandra.internal.GORM_ENHANCER_BEAN-cassandra"(CassandraGormEnhancer, ref("cassandraDatastore"), ref("cassandraTransactionManager")) { bean ->
                bean.initMethod = 'enhance'
                bean.destroyMethod = 'close'
                bean.lazyInit = false
            }
        }
    }
}
