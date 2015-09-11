
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
package grails.neo4j.bootstrap

import groovy.transform.InheritConstructors
import org.apache.tomcat.jdbc.pool.DataSource
import org.apache.tomcat.jdbc.pool.PoolProperties
import org.grails.config.PropertySourcesConfig
import org.grails.core.support.ClassEditor
import org.grails.datastore.gorm.bootstrap.AbstractDatastoreInitializer
import org.grails.datastore.gorm.neo4j.Neo4jGormEnhancer
import org.grails.datastore.gorm.neo4j.bean.factory.Neo4jDatastoreFactoryBean
import org.grails.datastore.gorm.neo4j.bean.factory.Neo4jMappingContextFactoryBean
import org.grails.datastore.gorm.neo4j.engine.JdbcCypherEngine
import org.springframework.beans.factory.config.CustomEditorConfigurer
import org.springframework.beans.factory.support.BeanDefinitionRegistry


/**
 * @author Graeme Rocher
 * @since 4.0
 */
@InheritConstructors
class Neo4jDataStoreSpringInitializer extends AbstractDatastoreInitializer {
    static String neo4jDefaultLocation = "data/neo4j"
    public static final String JDBC_NEO4J_PREFIX = "jdbc:neo4j:instance:"

    @Override
    Closure getBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry) {
        {->
            def config = configuration
            String neo4jUrl = config.getProperty('grails.neo4j.url', "jdbc:neo4j:mem")
            String username = config.getProperty('grails.neo4j.username', "")
            String password = config.getProperty('grails.neo4j.password', "")
            boolean isHaMode = config.getProperty('grails.neo4j.ha', Boolean, false)
            String neo4jLocation = config.getProperty('grails.neo4j.location', String, neo4jDefaultLocation)
            Map dbProperties = config.getProperty('grails.neo4j.dbProperties', Map, [:])

            def callable = getCommonConfiguration(beanDefinitionRegistry)
            callable.delegate = delegate
            callable.call()

            def m = neo4jUrl =~ /$JDBC_NEO4J_PREFIX(\w+)/

            if (m.matches()) {

                def instanceName = m[0][1]

                def factoryClassName = isHaMode ? "org.neo4j.graphdb.factory.HighlyAvailableGraphDatabaseFactory" : "org.neo4j.graphdb.factory.GraphDatabaseFactory"

                def factoryClazz = Thread.currentThread().contextClassLoader.loadClass(factoryClassName)

                def factoryMethodName = isHaMode ? "newHighlyAvailableDatabaseBuilder" : "newEmbeddedDatabaseBuilder"

                graphDbFactory(factoryClazz)
                graphDbBuilder(graphDbFactory : factoryMethodName,  neo4jLocation)

                graphDbBuilderFinal(graphDbBuilder: "setConfig", dbProperties ?: [:])
                graphDatabaseService(graphDbBuilderFinal: "newGraphDatabase") { bean ->
                    bean.destroyMethod = 'shutdown'
                }

                dbProperties[instanceName] = ref('graphDatabaseService')

            }

            neo4jPoolConfiguration(PoolProperties) {
                url = neo4jUrl
                driverClassName = "org.neo4j.jdbc.Driver"
                if (username) {
                    delegate.username = username
                }
                if (password) {
                    delegate.password = password
                }
                defaultAutoCommit = false  // important one!
                delegate.dbProperties = dbProperties
            }

            neo4jDataSource(DataSource, neo4jPoolConfiguration) { bean ->
                bean.destroyMethod = 'close'
            }

            cypherEngine(JdbcCypherEngine, neo4jDataSource)

            neo4jMappingContext(Neo4jMappingContextFactoryBean) {
                grailsApplication = ref('grailsApplication')
                defaultExternal = secondaryDatastore
            }

            neo4jDatastore(Neo4jDatastoreFactoryBean) {
                cypherEngine = cypherEngine
                mappingContext = neo4jMappingContext

            }

            // reverting the change done for fixing GRAILS-11112
            // since we supply a GraphDatabaseService instance to dbProperties we do not want
            // it being converted to a String
            customEditors(CustomEditorConfigurer) {
                customEditors = [(Class.name): ClassEditor.name ]
            }

            callable = getAdditionalBeansConfiguration(beanDefinitionRegistry, "neo4j")
            callable.delegate = delegate
            callable.call()

            "org.grails.gorm.neo4j.internal.GORM_ENHANCER_BEAN-neo4j"(Neo4jGormEnhancer, ref("neo4jDatastore"), ref("neo4jTransactionManager")) { bean ->
                bean.initMethod = 'enhance'
                bean.lazyInit = false
                includeExternal = !secondaryDatastore
            }
        }
    }
}
