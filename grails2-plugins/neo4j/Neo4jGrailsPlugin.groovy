import grails.neo4j.bootstrap.Neo4jDataStoreSpringInitializer
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsClass
import org.grails.datastore.mapping.web.support.OpenSessionInViewInterceptor
import org.springframework.beans.factory.support.BeanDefinitionRegistry

class Neo4jGrailsPlugin {
    def version = "6.0.0.BUILD-SNAPSHOT" // added by Gradle
    def license = "Apache 2.0 License"
    def organization = [name: "Grails", url: "http://grails.org/"]
    def developers = [
            [ name: "Stefan Armbruster", email: "stefan@armbruster-it.de" ],
            [name: "Graeme Rocher", email: "graeme@grails.org"]]
    def issueManagement = [system: "Github", url: "https://github.com/grails/grails-data-mapping"]
    def scm = [url: "https://github.com/grails/grails-data-mapping"]

    def grailsVersion = "2.5.0 > *"
    def observe = ['services', 'domainClass']
    def loadAfter = ['domainClass', 'hibernate', 'hibernate4', 'services']
    def author = "Stefan Armbruster"
    def authorEmail = "stefan@armbruster-it.de"
    def title = "Neo4j GORM"
    def description = 'A plugin that integrates the Neo4j graph datastore into Grails, providing a GORM API onto it'

    def documentation = "http://grails.github.io/grails-data-mapping/latest/neo4j/"

    def pluginExcludes = [
            'grails-app/domain/**'
    ]

    def doWithSpring = {
        def domainClasses = application.getArtefacts(DomainClassArtefactHandler.TYPE).collect() { GrailsClass cls -> cls.clazz }
        def initializer = new Neo4jDataStoreSpringInitializer(application.config, domainClasses)
        initializer.registerApplicationIfNotPresent = false
        initializer.setSecondaryDatastore( manager.hasGrailsPlugin("hibernate") || manager.hasGrailsPlugin("hibernate4")  )

        def definitions = initializer.getBeanDefinitions((BeanDefinitionRegistry) springConfig.getUnrefreshedApplicationContext())
        definitions.delegate = delegate
        definitions.call()

        def currentSpringConfig = getSpringConfig()
        if (manager?.hasGrailsPlugin("controllers")) {
            neo4jOpenSessionInViewInterceptor(OpenSessionInViewInterceptor) {
                datastore = ref("neo4jDatastore")
            }
            if (currentSpringConfig.containsBean("controllerHandlerMappings")) {
                controllerHandlerMappings.interceptors << ref("neo4jOpenSessionInViewInterceptor")
            }
            if (currentSpringConfig.containsBean("annotationHandlerMapping")) {
                if (annotationHandlerMapping.interceptors) {
                    annotationHandlerMapping.interceptors << ref("neo4jOpenSessionInViewInterceptor")
                }
                else {
                    annotationHandlerMapping.interceptors = [ref("neo4jOpenSessionInViewInterceptor")]
                }
            }
        }
    }
}
