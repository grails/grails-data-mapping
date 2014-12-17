package grails.plugin.hibernate

import grails.config.Config
import grails.core.GrailsApplication
import grails.core.GrailsClass
import grails.core.GrailsDomainClassProperty
import grails.core.support.GrailsApplicationAware
import grails.orm.bootstrap.HibernateDatastoreSpringInitializer
import grails.plugins.Plugin
import groovy.transform.CompileStatic
import org.grails.core.artefact.DomainClassArtefactHandler
import org.springframework.beans.factory.support.BeanDefinitionRegistry

/**
 * Plugin that integrates Hibernate into a Grails application
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class HibernateGrailsPlugin extends Plugin {


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


    Closure doWithSpring() {{->
        GrailsApplication grailsApplication = grailsApplication
        Set<String> datasourceNames = []

        Config config = grailsApplication.config
        Map dataSources = config.getProperty('dataSources', Map, [:])

        if(dataSources) {
            for (name in config.keySet()) {
                if(name == 'dataSource') {
                    datasourceNames << GrailsDomainClassProperty.DEFAULT_DATA_SOURCE
                }
                else {
                    datasourceNames << name
                }
            }
        }
        else {
            Map dataSource = config.getProperty('dataSource', Map, [:])
            if(dataSource) {
                datasourceNames << GrailsDomainClassProperty.DEFAULT_DATA_SOURCE
            }
        }


        def springInitializer = new HibernateDatastoreSpringInitializer(config, grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE).collect() { GrailsClass cls -> cls.clazz })
        springInitializer.registerApplicationIfNotPresent = false
        springInitializer.dataSources = datasourceNames
        def beans = springInitializer.getBeanDefinitions((BeanDefinitionRegistry)applicationContext)

        beans.delegate = delegate
        beans.call()
    }}

}
