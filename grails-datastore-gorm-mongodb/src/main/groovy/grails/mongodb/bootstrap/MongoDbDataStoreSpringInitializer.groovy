/* Copyright (C) 2014 SpringSource
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
package grails.mongodb.bootstrap

import com.mongodb.DBAddress
import com.mongodb.Mongo
import com.mongodb.MongoOptions
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.InstanceFactoryBean
import org.grails.datastore.gorm.bootstrap.AbstractDatastoreInitializer
import org.grails.datastore.gorm.mongo.MongoGormEnhancer
import org.grails.datastore.gorm.mongo.bean.factory.GMongoFactoryBean
import org.grails.datastore.gorm.mongo.bean.factory.MongoDatastoreFactoryBean
import org.grails.datastore.gorm.mongo.bean.factory.MongoMappingContextFactoryBean
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.data.mongodb.core.MongoOptionsFactoryBean

/**
 * Used to initialize GORM for MongoDB outside of Grails
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class MongoDbDataStoreSpringInitializer extends AbstractDatastoreInitializer{

    public static final String DEFAULT_DATABASE_NAME = "test"
    protected String mongoBeanName = "mongo"
    protected String mongoOptionsBeanName = "mongoOptions"
    protected String databaseName = DEFAULT_DATABASE_NAME
    protected Closure defaultMapping
    protected MongoOptions mongoOptions
    protected Mongo mongo


    MongoDbDataStoreSpringInitializer() {
    }

    MongoDbDataStoreSpringInitializer(ClassLoader classLoader, String... packages) {
        super(classLoader, packages)
    }

    MongoDbDataStoreSpringInitializer(String... packages) {
        super(packages)
    }

    MongoDbDataStoreSpringInitializer(Collection<Class> persistentClasses) {
        super(persistentClasses)
    }

    MongoDbDataStoreSpringInitializer(Class... persistentClasses) {
        super(persistentClasses)
    }

    MongoDbDataStoreSpringInitializer(Properties configuration, Collection<Class> persistentClasses) {
        super(configuration, persistentClasses)
    }

    MongoDbDataStoreSpringInitializer(Properties configuration, Class... persistentClasses) {
        super(configuration, persistentClasses)
    }

    /**
     * Configures for an existing Mongo instance
     * @param mongo The instance of Mongo
     * @return The configured ApplicationContext
     */
    @CompileStatic
    ApplicationContext configure() {
        ExpandoMetaClass.enableGlobally()
        GenericApplicationContext applicationContext = new GenericApplicationContext()
        applicationContext.beanFactory.registerSingleton( mongoBeanName, mongo)
        configureForBeanDefinitionRegistry(applicationContext)
        applicationContext.refresh()
        return applicationContext
    }

    @Override
    Closure getBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry) {
        return {
            def config = configuration ? new ConfigSlurper().parse(configuration) : new ConfigObject()
            def mongoConfig = config?.grails?.mongo?.clone() ?: config?.grails?.mongodb?.clone()
            if(mongoConfig == null) mongoConfig = new ConfigObject()

            def callable = getCommonConfiguration(beanDefinitionRegistry)
            callable.delegate = delegate
            callable.call()

            mongoMappingContext(MongoMappingContextFactoryBean) {
                defaultDatabaseName = databaseName
                grailsApplication = ref(GrailsApplication.APPLICATION_ID)
                if (mongoConfig.default.mapping instanceof Closure) {
                    delegate.defaultMapping = this.defaultMapping
                }
            }

            if(mongoOptions) {
                "$mongoOptionsBeanName"(InstanceFactoryBean, mongoOptions)
            }
            else if(!beanDefinitionRegistry.containsBeanDefinition(mongoOptionsBeanName)) {
                "$mongoOptionsBeanName"(MongoOptionsFactoryBean) {
                    if (mongoConfig?.options) {
                        def options = mongoConfig.remove("options")
                        if(options instanceof Map) {
                            for (option in options) {
                                setProperty(option.key, option.value)
                            }
                        }
                    }
                }
            }

            if(mongo) {
                "$mongoBeanName"(InstanceFactoryBean, mongo)
            }
            else if(!beanDefinitionRegistry.containsBeanDefinition(mongoOptionsBeanName)) {

                "gmongo"(GMongoFactoryBean) {
                    delegate.mongoOptions = ref("$mongoOptionsBeanName")
                    def mongoHost = mongoConfig?.get("host")

                    if (mongoConfig?.replicaSet) {
                        def set = []
                        for (server in mongoConfig.get("replicaSet")) {
                            set << new DBAddress(server.indexOf("/") > 0 ? server : "$server/$databaseName")
                        }

                        replicaSetSeeds = set
                    }
                    else if (mongoConfig?.replicaPair) {
                        def pair = []
                        for (server in mongoConfig.get("replicaPair")) {
                            pair << new DBAddress(server.indexOf("/") > 0 ? server : "$server/$databaseName")
                        }
                        replicaPair = pair
                    }
                    else if(mongoConfig?.connectionString) {
                        connectionString = mongoConfig.connectionString.toString()
                    }
                    else if (mongoHost) {
                        host = mongoHost
                        def mongoPort = mongoConfig?.get("port")
                        if (mongoPort) port = mongoPort
                    }
                    else {
                        host = "localhost"
                    }
                }

                "$mongoBeanName"(gmongo:"getMongo")
            }
            mongoDatastore(MongoDatastoreFactoryBean) {
                delegate.mongo = ref(mongoBeanName)
                mappingContext = mongoMappingContext
                config = mongoConfig.toProperties()
            }
            "mongoTransactionManager"(DatastoreTransactionManager) {
                datastore = ref("mongoDatastore")
            }

            "org.grails.gorm.mongodb.internal.GORM_ENHANCER_BEAN-${mongoBeanName}"(MongoGormEnhancer, ref("mongoDatastore"), ref("mongoTransactionManager")) { bean ->
                bean.initMethod = 'enhance'
                bean.lazyInit = false
            }
        }
    }

    /**
     * Sets the name of the Mongo bean to use
     */
    void setMongoBeanName(String mongoBeanName) {
        this.mongoBeanName = mongoBeanName
    }
    /**
     * The name of the MongoOptions bean
     *
     * @param mongoOptionsBeanName The mongo options bean name
     */
    void setMongoOptionsBeanName(String mongoOptionsBeanName) {
        this.mongoOptionsBeanName = mongoOptionsBeanName
    }
    /**
     * Sets the MongoOptions instance to use when constructing the Mongo instance
     */
    void setMongoOptions(MongoOptions mongoOptions) {
        this.mongoOptions = mongoOptions
    }
    /**
     * Sets a pre-existing Mongo instance to configure for
     * @param mongo The Mongo instance
     */
    void setMongo(Mongo mongo) {
        this.mongo = mongo
    }
    /**
     * Sets the name of the MongoDB database to use
     */
    void setDatabaseName(String databaseName) {
        this.databaseName = databaseName
    }

    /**
     * Sets the default MongoDB GORM mapping configuration
     */
    void setDefaultMapping(Closure defaultMapping) {
        this.defaultMapping = defaultMapping
    }
}
