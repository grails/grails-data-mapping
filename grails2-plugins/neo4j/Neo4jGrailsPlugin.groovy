import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.springframework.beans.factory.support.BeanDefinitionRegistry

class Neo4jGrailsPlugin {
    def version = "5.0.0.BUILD-SNAPSHOT" // added by Gradle
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
        def initializer = new grails.neo4j.bootstrap.Neo4jDataStoreSpringInitializer(application.config, application.getArtefacts(DomainClassArtefactHandler.TYPE).collect() { org.codehaus.groovy.grails.commons.GrailsClass cls -> cls.clazz })
        initializer.registerApplicationIfNotPresent = false
        initializer.setSecondaryDatastore( manager.hasGrailsPlugin("hibernate")  )

        def definitions = initializer.getBeanDefinitions((BeanDefinitionRegistry) springConfig.getUnrefreshedApplicationContext())
        definitions.delegate = delegate
        definitions.call()
    }
}
