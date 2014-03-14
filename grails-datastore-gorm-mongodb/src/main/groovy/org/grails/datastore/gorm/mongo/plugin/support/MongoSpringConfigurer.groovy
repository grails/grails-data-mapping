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
package org.grails.datastore.gorm.mongo.plugin.support

import org.grails.datastore.gorm.mongo.bean.factory.DefaultMappingHolder
import org.grails.datastore.gorm.mongo.bean.factory.GMongoFactoryBean
import org.grails.datastore.gorm.mongo.bean.factory.MongoDatastoreFactoryBean
import org.grails.datastore.gorm.mongo.bean.factory.MongoMappingContextFactoryBean
import org.grails.datastore.gorm.plugin.support.SpringConfigurer
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.data.mongodb.core.MongoOptionsFactoryBean

import com.mongodb.DBAddress

/**
 * Mongo specific configuration logic for Spring
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class MongoSpringConfigurer extends SpringConfigurer {
    @Override
    String getDatastoreType() {
        return "Mongo"
    }

    @Override
    Closure getSpringCustomizer() {
        return {
            def mongoConfig = application.config?.grails?.mongo?.clone() ?: application.config?.grails?.mongodb?.clone()
            if(mongoConfig == null) mongoConfig = new ConfigObject()
            def databaseName = mongoConfig?.remove("databaseName") ?: application.metadata.getApplicationName()
            "${databaseName}DB"(MethodInvokingFactoryBean) { bean ->
                bean.scope = "request"
                targetObject = ref("mongo")
                targetMethod = "getDB"
                arguments = [databaseName]
            }

            mongoMappingContext(MongoMappingContextFactoryBean) {
                defaultDatabaseName = databaseName
                grailsApplication = ref('grailsApplication')
                pluginManager = ref('pluginManager')
                if (mongoConfig.default.mapping instanceof Closure) {
                    defaultMapping = new DefaultMappingHolder((Closure)mongoConfig.default.mapping)
                }
            }

            mongoOptions(MongoOptionsFactoryBean) {
                if (mongoConfig?.options) {
                    for (option in mongoConfig.remove("options")) {
                        setProperty(option.key, option.value)
                    }
                }
            }

            mongo(GMongoFactoryBean) {
                mongoOptions = mongoOptions
                def mongoHost = mongoConfig?.remove("host")

                if (mongoConfig?.replicaSet) {
                    def set = []
                    for (server in mongoConfig.remove("replicaSet")) {
                        set << new DBAddress(server.indexOf("/") > 0 ? server : "$server/$databaseName")
                    }

                    replicaSetSeeds = set
                }
                else if (mongoConfig?.replicaPair) {
                    def pair = []
                    for (server in mongoConfig.remove("replicaPair")) {
                        pair << new DBAddress(server.indexOf("/") > 0 ? server : "$server/$databaseName")
                    }
                    replicaPair = pair
                }
                else if(mongoConfig?.connectionString) {
                    connectionString = mongoConfig.connectionString.toString()
                }
                else if (mongoHost) {
                    host = mongoHost
                    def mongoPort = mongoConfig?.remove("port")
                    if (mongoPort) port = mongoPort
                }
            }
            mongoBean(mongo: "getMongo")
            mongoDatastore(MongoDatastoreFactoryBean) {
                mongo = ref("mongoBean")
                mappingContext = mongoMappingContext
                config = mongoConfig.toProperties()
            }
        }
    }
}
