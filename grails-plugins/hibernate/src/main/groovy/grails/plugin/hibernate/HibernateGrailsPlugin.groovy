package grails.plugin.hibernate

import grails.core.GrailsApplication
import grails.core.GrailsClass
import grails.core.GrailsDomainClassProperty
import grails.orm.bootstrap.HibernateDatastoreSpringInitializer
import org.grails.core.artefact.DomainClassArtefactHandler

/**
 * Plugin that integrates Hibernate into a Grails application
 *
 * @author Graeme Rocher
 * @since 3.0
 */
class HibernateGrailsPlugin {


    def version = '4.3.5.5-SNAPSHOT'
    def grailsVersion = '2.3.5 > *'
    def author = 'Burt Beckwith'
    def title = 'Hibernate 4 for Grails'
    def description = 'Provides integration between Grails and Hibernate 4 through GORM'
    def documentation = 'http://grails.org/plugin/hibernate4'

    def observe = ['domainClass']
    def loadAfter = ['controllers', 'domainClass']
    def watchedResources = ['file:./grails-app/conf/hibernate/**.xml']
    def pluginExcludes = ['src/templates/**']

    def license = 'APACHE'
    def organization = [name: 'Pivotal', url: 'http://pivotal.io']
    def issueManagement = [system: 'JIRA', url: 'http://jira.grails.org/browse/GPHIB']
    def scm = [url: 'https://github.com/grails-plugins/grails-hibernate4-plugin']

    def doWithSpring = {
        GrailsApplication grailsApplication = application
        def datasourceNames = []
        if (getSpringConfig().containsBean('dataSource')) {
            datasourceNames << GrailsDomainClassProperty.DEFAULT_DATA_SOURCE
        }

        for (name in grailsApplication.config.keySet()) {
            if (name.startsWith('dataSource_')) {
                datasourceNames << name - 'dataSource_'
            }
        }

        def springInitializer = new HibernateDatastoreSpringInitializer(grailsApplication.config.toProperties(), grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE).collect() { GrailsClass cls -> cls.clazz })
        springInitializer.registerApplicationIfNotPresent = false
        springInitializer.dataSources = datasourceNames
        def beans = springInitializer.getBeanDefinitions(getSpringConfig().getUnrefreshedApplicationContext())

        beans.delegate = delegate
        beans.call()
    }

}
