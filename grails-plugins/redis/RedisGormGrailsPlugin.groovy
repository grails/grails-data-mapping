/* Copyright (C) 2010 SpringSource
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

import org.grails.datastore.gorm.redis.plugin.support.*
import org.grails.datastore.gorm.plugin.support.*

class RedisGormGrailsPlugin {
    def license = "Apache 2.0 License"
    def organization = [ name: "SpringSource", url: "http://www.springsource.org/" ]
    def developers = [
        [ name: "Graeme Rocher", email: "grocher@vmware.com" ] ]
    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPREDIS" ]
    def scm = [ url: "https://github.com/SpringSource/grails-data-mapping" ]

    def version = "1.0.0.M7"
    def grailsVersion = "1.3.4 > *"
    def dependsOn = [redis:"1.0.0.M7 > *"]
    def loadAfter = ['domainClass', 'hibernate', 'services', 'cloudFoundry', 'redis']

    def author = "Graeme Rocher"
    def authorEmail = "graeme.rocher@springsource.com"
    def title = "Redis GORM"
    def description = '''\\
A plugin that integrates the Redis key/value datastore into Grails, providing
a GORM-like API onto it
'''

    def pluginExcludes = [
        "grails-app/domain/*.groovy",
        "grails-app/services/*.groovy",
        "grails-app/controllers/*.groovy"
    ]

    def documentation = "http://grails.org/plugin/redis"

    def doWithSpring = new RedisSpringConfigurer().getConfiguration()
    
    def doWithDynamicMethods = { ctx ->
        def datastore = ctx.redisDatastore
        def transactionManager = ctx.redisDatastoreTransactionManager
        def methodsConfigurer = new RedisMethodsConfigurer(datastore, transactionManager)    
        methodsConfigurer.hasExistingDatastore = manager.hasGrailsPlugin("hibernate")        
        methodsConfigurer.configure()
    }

    def doWithApplicationContext = { ctx ->
        new ApplicationContextConfigurer("Redis").configure(ctx)
    }
    
    def onChange = {
        def onChangeHandler = new RedisOnChangeHandler()
        onChangeHandler.onChange(delegate, event)        
    }
}
