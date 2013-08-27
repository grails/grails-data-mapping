/* Copyright (C) 2013 original authors
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
import org.grails.datastore.gorm.rest.client.plugin.support.*

/**
 * @author Graeme Rocher
 */
class GormRestClientGrailsPlugin {
    // the plugin version
    def version = "1.0.0.M1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.3 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "GORM Rest Client Plugin" // Headline display name of the plugin
    def author = "Graeme Rocher"
    def authorEmail = "graeme.rocher@gmail.com"
    def description = '''\
A GORM implementation that can back onto a REST web service 
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/gorm-rest-client"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
    def organization = [ name: "SpringSource", url: "http://www.springsource.com/" ]

    // Any additional developers beyond the author specified above.
    def developers = [ [ name: "Graeme Rocher", email: "graeme.rocher@gmail.com" ]]

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GRAILS" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "https://github.com/SpringSource/grails-data-mapping" ]

    def doWithSpring = new RestClientSpringConfigurer().getConfiguration()

    def doWithDynamicMethods = { ctx ->
      def datastore = ctx.mongoDatastore
        def transactionManager = ctx.restClientTransactionManager
        def methodsConfigurer = new RestClientMethodsConfigurer(datastore, transactionManager)    
        methodsConfigurer.hasExistingDatastore = manager.hasGrailsPlugin("hibernate")        
        def foe = application?.config?.grails?.gorm?.failOnError
        methodsConfigurer.failOnError = foe instanceof Boolean ? foe : false
        methodsConfigurer.configure()
    }
}
