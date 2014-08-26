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
package org.grails.datastore.gorm.neo4j.plugin.support

import org.apache.tomcat.jdbc.pool.PoolProperties
import org.grails.core.support.ClassEditor
import org.grails.datastore.gorm.neo4j.engine.JdbcCypherEngine
import org.grails.datastore.gorm.plugin.support.SpringConfigurer
import org.grails.datastore.gorm.neo4j.bean.factory.Neo4jMappingContextFactoryBean
import org.grails.datastore.gorm.neo4j.bean.factory.Neo4jDatastoreFactoryBean

import org.apache.tomcat.jdbc.pool.DataSource
import org.springframework.beans.factory.config.CustomEditorConfigurer

/**
 * Spring configurer for Neo4j
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class Neo4jSpringConfigurer extends SpringConfigurer {

    static neo4jDefaultLocation = "data/neo4j"
    public static final String JDBC_NEO4J_PREFIX = "jdbc:neo4j:instance:"


    @Override
    String getDatastoreType() {
        return "Neo4j"
    }

    @Override
    Closure getSpringCustomizer() {
        return {

            def config = application.config?.grails?.neo4j ?: new ConfigObject()

            def neo4jUrl = config?.url ?: "jdbc:neo4j:mem"
            def neo4jProperties = [:]

            def m = neo4jUrl =~ /$JDBC_NEO4J_PREFIX(\w+)/

            if (m.matches()) {

                def instanceName = m[0][1]
                def isHaMode = config.ha ?: false

                def factoryClassName = isHaMode ? "org.neo4j.graphdb.factory.HighlyAvailableGraphDatabaseFactory" : "org.neo4j.graphdb.factory.GraphDatabaseFactory"

                def factoryClazz = Thread.currentThread().contextClassLoader.loadClass(factoryClassName)

                def factoryMethodName = isHaMode ? "newHighlyAvailableDatabaseBuilder" : "newEmbeddedDatabaseBuilder"

                graphDbFactory(factoryClazz)
                graphDbBuilder(graphDbFactory : factoryMethodName,  config.location ?: neo4jDefaultLocation)

                graphDbBuilderFinal(graphDbBuilder: "setConfig", config.dbProperties ?: [:])
                graphDatabaseService(graphDbBuilderFinal: "newGraphDatabase") { bean ->
                    bean.destroyMethod = 'shutdown'
                }

                neo4jProperties[instanceName] = ref('graphDatabaseService')

            }
            neo4jProperties.putAll(config.dbProperties)

            neo4jPoolConfiguration(PoolProperties) {
                url = neo4jUrl
                driverClassName = "org.neo4j.jdbc.Driver"
                if (config.username) {
                    username = config.username
                }
                if (config.password) {
                    password = config.password
                }
                defaultAutoCommit = false  // important one!
                dbProperties = neo4jProperties
            }

            neo4jDataSource(DataSource, neo4jPoolConfiguration) { bean ->
                bean.destroyMethod = 'close'
            }

            cypherEngine(JdbcCypherEngine, neo4jDataSource)

            neo4jMappingContext(Neo4jMappingContextFactoryBean) {
                grailsApplication = ref('grailsApplication')
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

        }
    }
}
