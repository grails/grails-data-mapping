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

import org.grails.datastore.gorm.plugin.support.SpringConfigurer
import org.grails.datastore.gorm.neo4j.bean.factory.Neo4jMappingContextFactoryBean
import org.grails.datastore.gorm.neo4j.bean.factory.Neo4jDatastoreFactoryBean

/**
 * Spring configurer for Neo4j
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class Neo4jSpringConfigurer extends SpringConfigurer {
    @Override
    String getDatastoreType() {
        return "Neo4j"
    }

    @Override
    Closure getSpringCustomizer() {
        return {
            def neo4jConfig = application.config?.grails?.neo4j  // use config from app's Datasource.groovy
            if (!neo4jConfig) {
                throw new IllegalArgumentException("Unable to find 'grails.neo4j' in application config.")
            }
            Class neo4jGraphDatabaseClass

            if (neo4jConfig.type == "rest") {
                neo4jGraphDatabaseClass = "org.neo4j.rest.graphdb.RestGraphDatabase" as Class

                // env paramters have precedence (Heroku uses this)
                def location = System.env['NEO4J_HOST'] ?
                    "http://${System.env['NEO4J_HOST']}:${System.env['NEO4J_PORT']}/db/data" :
                    neo4jConfig.location ?: "http://localhost:7474/db/data/"
                def login = System.env['NEO4J_LOGIN'] ?: neo4jConfig.login ?: null
                def password = System.env['NEO4J_PASSWORD'] ?: neo4jConfig.password ?: null

                graphDatabaseService(neo4jGraphDatabaseClass, location, login, password) { bean ->
                    bean.destroyMethod = "shutdown"
                }
            } else {
                String neo4jGraphDatabaseClassName
                String neo4jDefaultLocation
                switch (neo4jConfig.type) {
                    case "ha":
                        neo4jGraphDatabaseClassName = "org.neo4j.kernel.HighlyAvailableGraphDatabase"
                        neo4jDefaultLocation = "data/neo4j"
                        break
                    case "embedded":
                        neo4jGraphDatabaseClassName = "org.neo4j.kernel.EmbeddedGraphDatabase"
                        neo4jDefaultLocation = "data/neo4j"
                        break
                    case "impermanent":
                        neo4jGraphDatabaseClassName = "org.neo4j.test.ImpermanentGraphDatabase"
                        neo4jDefaultLocation = "data/neo4j"
                        break
                    default:  // otherwise type is used as classname
                        if (neo4jConfig.type) {
                            neo4jGraphDatabaseClassName = neo4jConfig.type
                            neo4jDefaultLocation = "data/neo4j"
                        } else {
                            throw new RuntimeException("no config for neo4j found")
                        }
                        break
                }

                try {
                    neo4jGraphDatabaseClass = neo4jGraphDatabaseClassName as Class
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("could not load $neo4jGraphDatabaseClassName, maybe add neo4j-enterprise to dependecies section", e)
                }

                graphDatabaseService(
                        neo4jGraphDatabaseClass,
                        neo4jConfig.location ?: neo4jDefaultLocation,
                        neo4jConfig.params ?: [:]
                ) { bean ->
                    bean.destroyMethod = "shutdown"
                }

            }

            neo4jMappingContext(Neo4jMappingContextFactoryBean) {
                grailsApplication = ref('grailsApplication')
                pluginManager = ref('pluginManager')
            }

            neo4jDatastore(Neo4jDatastoreFactoryBean) {
                graphDatabaseService = graphDatabaseService
                mappingContext = neo4jMappingContext

            }

        if (manager?.hasGrailsPlugin("controllers")) {
/*
            neo4jOpenSessionInViewInterceptor(Neo4jOpenSessionInViewInterceptor) {
                datastore = ref("neo4jDatastore")
            }
*/
        }

        }
    }
}
