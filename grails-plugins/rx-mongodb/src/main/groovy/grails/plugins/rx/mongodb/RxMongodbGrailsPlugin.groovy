package grails.plugins.rx.mongodb

import grails.core.GrailsClass
import grails.gorm.rx.mongodb.RxMongoEntity
import grails.plugins.Plugin
import groovy.transform.CompileStatic
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import org.grails.plugins.web.rx.mvc.RxResultTransformer

/**
 *
 *
 * @author Graeme Rocher
 * @since 6.0
 */
class RxMongodbGrailsPlugin extends Plugin {

    def license = "Apache 2.0 License"
    def organization = [name: "Grails", url: "http://grails.org/"]
    def developers = [
            [name: "Graeme Rocher", email: "graeme@grails.org"]]
    def issueManagement = [system: "Github", url: "https://github.com/grails/grails-data-mapping"]
    def scm = [url: "https://github.com/grails/grails-data-mapping"]

    def grailsVersion = "3.0.0 > *"
    def observe = ['services', 'domainClass']
    def loadAfter = ['domainClass', 'hibernate', 'hibernate4', 'services']
    def author = "Graeme Rocher"
    def authorEmail = "graeme@grails.org"
    def title = "MongoDB RxGORM"
    def description = 'A plugin that integrates the MongoDB document datastore into Grails, providing a RxGORM API onto it'

    def documentation = "http://grails.github.io/grails-data-mapping/latest/mongodb/"

    @Override
    Closure doWithSpring() {
        {->
            def applicationName = grailsApplication.getMetadata().getApplicationName()
            def classes = grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE).findAll() { GrailsClass cls ->
                RxMongoEntity.isAssignableFrom(cls.clazz)
            }.collect() { GrailsClass cls -> cls.clazz }
            rxMongoDatastoreClient(RxMongoDatastoreClient, config, applicationName, classes as Class[])
            rxAsyncResultTransformer(RxResultTransformer)
        }
    }
}
