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
import com.mongodb.MongoClientOptions
import com.mongodb.MongoClientURI
import grails.mongodb.MongoEntity
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.grails.datastore.gorm.bootstrap.AbstractDatastoreInitializer
import org.grails.datastore.gorm.bootstrap.support.InstanceFactoryBean
import org.grails.datastore.gorm.mongo.MongoGormEnhancer
import org.grails.datastore.gorm.mongo.bean.factory.*
import org.grails.datastore.gorm.support.AbstractDatastorePersistenceContextInterceptor
import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericApplicationContext
import static org.grails.datastore.mapping.mongo.MongoDatastore.*
/**
 * Used to initialize GORM for MongoDB outside of Grails
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@InheritConstructors
class MongoDbDataStoreSpringInitializer extends AbstractDatastoreInitializer {

    public static final String DEFAULT_DATABASE_NAME = "test"


    protected String mongoBeanName = "mongo"
    protected String mongoOptionsBeanName = "mongoOptions"
    protected String databaseName = DEFAULT_DATABASE_NAME
    protected Closure defaultMapping
    protected MongoClientOptions mongoOptions
    protected Mongo mongo

    @Override
    protected Class<AbstractDatastorePersistenceContextInterceptor> getPersistenceInterceptorClass() {
        DatastorePersistenceContextInterceptor
    }

    @Override
    protected boolean isMappedClass(String datastoreType, Class cls) {
        return MongoEntity.isAssignableFrom(cls) || super.isMappedClass(datastoreType, cls)
    }

    /**
     * Configures for an existing Mongo instance
     * @param mongo The instance of Mongo
     * @return The configured ApplicationContext
     */
    @CompileStatic
    ApplicationContext configure() {
        GenericApplicationContext applicationContext = new GenericApplicationContext()
        applicationContext.beanFactory.registerSingleton( mongoBeanName, mongo)
        configureForBeanDefinitionRegistry(applicationContext)
        applicationContext.refresh()
        return applicationContext
    }

    @Override
    Closure getBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry) {
        return {
            final config = configuration
            String connectionString = config.getProperty(SETTING_CONNECTION_STRING,config.getProperty(SETTING_URL,'')) ?: null
            databaseName = config.getProperty(SETTING_DATABASE_NAME, databaseName)
            Closure defaultMapping = config.getProperty(SETTING_DEFAULT_MAPPING,Closure, this.defaultMapping)
            Map mongoOptions = config.getProperty(SETTING_OPTIONS, Map, null)
            String hostSetting = config.getProperty(SETTING_HOST, '')
            Integer mongoPort = config.getProperty(SETTING_PORT, Integer, null)
            String username = config.getProperty(SETTING_USERNAME, '')
            String password= config.getProperty(SETTING_PASSWORD, '')
            Collection<String> replicaSetSetting = config.getProperty(SETTING_REPLICA_SET, Collection, [])
            Collection<String> replicaPairSetting = config.getProperty(SETTING_REPLICA_PAIR, Collection, [])

            MongoClientURI mongoClientURI = null
            if(connectionString) {
                mongoClientURI = new MongoClientURI(connectionString)
                databaseName = mongoClientURI.database
            }

            def callable = getCommonConfiguration(beanDefinitionRegistry, "mongo")
            callable.delegate = delegate
            callable.call()

            gormMongoMappingContext(MongoMappingContextFactoryBean) {
                defaultDatabaseName = databaseName
                grailsApplication = ref("grailsApplication")
                if (defaultMapping) {
                    delegate.defaultMapping = new DefaultMappingHolder(defaultMapping)
                }
                defaultExternal = secondaryDatastore
            }

            if(this.mongoOptions) {
                "$mongoOptionsBeanName"(InstanceFactoryBean, this.mongoOptions, MongoClientOptions)
            }
            else if(!beanDefinitionRegistry.containsBeanDefinition(mongoOptionsBeanName)) {
                "$mongoOptionsBeanName"(MongoClientOptionsFactoryBean) {
                    if(mongoOptions) {
                        delegate.mongoOptions = mongoOptions
                    }
                }
            }

            if(mongo) {
                "$mongoBeanName"(InstanceFactoryBean, mongo)
            }
            else  {

                def existingBean = beanDefinitionRegistry.containsBeanDefinition(mongoBeanName) ? beanDefinitionRegistry.getBeanDefinition(mongoBeanName) : null
                boolean registerMongoBean = false
                if(existingBean instanceof AnnotatedBeanDefinition) {
                    AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition)existingBean
                    if(annotatedBeanDefinition.metadata.className == 'org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration') {
                        registerMongoBean = true
                    }
                }
                else if(existingBean == null) {
                    registerMongoBean = true
                }

                if(registerMongoBean) {
                    "$mongoBeanName"(MongoClientFactoryBean) {
                        delegate.mongoOptions = ref("$mongoOptionsBeanName")
                        delegate.database = databaseName
                        if(username && password) {
                            delegate.username = username
                            delegate.password = password
                        }

                        if(mongoClientURI) {
                            clientURI = mongoClientURI
                        }
                        else if (replicaSetSetting) {
                            def set = []
                            for (server in replicaSetSetting) {
                                set << new DBAddress(server.indexOf("/") > 0 ? server : "$server/$databaseName")
                            }

                            replicaSetSeeds = set
                        }
                        else if (replicaPairSetting) {
                            def pair = []
                            for (server in replicaPairSetting) {
                                pair << new DBAddress(server.indexOf("/") > 0 ? server : "$server/$databaseName")
                            }
                            replicaPair = pair
                        }
                        else if (hostSetting) {
                            host = hostSetting
                            if (mongoPort) {
                                port = mongoPort
                            }
                        }
                        else {
                            host = "localhost"
                        }
                    }

                }

            }
            mongoDatastore(MongoDatastoreFactoryBean) {
                delegate.mongo = ref(mongoBeanName)
                mappingContext = gormMongoMappingContext
                delegate.config = config
            }

            callable = getAdditionalBeansConfiguration(beanDefinitionRegistry, "mongo")
            callable.delegate = delegate
            callable.call()

            "org.grails.gorm.mongodb.internal.GORM_ENHANCER_BEAN-${mongoBeanName}"(MongoGormEnhancer, ref("mongoDatastore"), ref("mongoTransactionManager"), config.getProperty(SETTING_FAIL_ON_ERROR, Boolean, false)) { bean ->
                bean.initMethod = 'enhance'
                bean.destroyMethod = 'close'
                bean.lazyInit = false
                includeExternal = !secondaryDatastore
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
    void setMongoOptions(MongoClientOptions mongoOptions) {
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
