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

import org.grails.datastore.gorm.neo4j.engine.JdbcCypherEngine
import org.grails.datastore.gorm.plugin.support.SpringConfigurer
import org.grails.datastore.gorm.neo4j.bean.factory.Neo4jMappingContextFactoryBean
import org.grails.datastore.gorm.neo4j.bean.factory.Neo4jDatastoreFactoryBean
import org.neo4j.graphdb.factory.GraphDatabaseFactory

/**
 * Spring configurer for Neo4j
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class Neo4jSpringConfigurer extends SpringConfigurer {

    static neo4jDefaultLocation = "data/neo4j"

    @Override
    String getDatastoreType() {
        return "Neo4j"
    }

    @Override
    Closure getSpringCustomizer() {
        return {

            def url = application.config.dataSource.url
            if (url.startsWith(JDBCConfigurationInterceptor.JDBC_NEO4J_PREFIX)) {

                graphDbFactory(GraphDatabaseFactory)
                graphDbBuilder(graphDbFactory : "newEmbeddedDatabaseBuilder",  /*neo4jConfig.location ?: */ neo4jDefaultLocation)

                graphDbBuilderFinal(graphDbBuilder: "setConfig", /*neo4jConfig.params ?:*/ [:])
                graphDatabaseService(graphDbBuilderFinal: "newGraphDatabase") { bean ->
                    bean.destroyMethod = 'shutdown'
                }

                // create dummy entry for datasource's dbProperties
                // gets overridden by jdbcConfigurationInterceptor
                Properties props = new Properties();
                props.put("dataSource.properties.dbProperties.dummy", "dummy");
                application.config.merge(new ConfigSlurper().parse(props));

                jdbcConfigurationInterceptor(JDBCConfigurationInterceptor) {
                    graphDatabaseService = graphDatabaseService
                    config = application.config
                }
            }

            cypherEngine(JdbcCypherEngine, ref('dataSource'))

            neo4jMappingContext(Neo4jMappingContextFactoryBean) {
                grailsApplication = ref('grailsApplication')
            }

            neo4jDatastore(Neo4jDatastoreFactoryBean) {
                cypherEngine = cypherEngine
                mappingContext = neo4jMappingContext

            }

/*        if (manager?.hasGrailsPlugin("controllers")) {

            neo4jOpenSessionInViewInterceptor(Neo4jOpenSessionInViewInterceptor) {
                datastore = ref("neo4jDatastore")
            }

        }*/

        }
    }
}
