package org.grails.datastore.gorm.redis

import grails.core.GrailsClass
import grails.plugins.Plugin
import grails.redis.bootstrap.RedisDatastoreSpringInitializer
import groovy.transform.CompileStatic
import org.grails.core.artefact.DomainClassArtefactHandler
import org.springframework.beans.factory.support.BeanDefinitionRegistry

class RedisGrailsPlugin extends Plugin {

    def license = "Apache 2.0 License"
    def organization = [ name: "Grails", url: "http://grails.org/" ]
    def developers = [
            [ name: "Graeme Rocher", email: "graeme@grails.org"] ]
    def issueManagement = [ system: "JIRA", url: "https://github.com/grails/grails-data-mapping/issues" ]
    def scm = [ url: "https://github.com/grails/grails-data-mapping" ]

    def grailsVersion = "3.0.0 > *"
    def loadAfter = ['domainClass', 'hibernate', 'services', 'converters']
    //def loadBefore = ['dataSource']
    def observe = ['services', 'domainClass']

    def author = "Graeme Rocher"
    def authorEmail = "graeme@grails.org"
    def title = "Redis GORM"
    def description = 'A plugin that integrates the Redis database into Grails, providing a GORM API onto it'

    def documentation = "http://grails.github.io/grails-data-mapping/latest/redis"

    def dependsOn = [:]
    // resources that are excluded from plugin packaging

    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    @Override
    @CompileStatic
    Closure doWithSpring() {
        def initializer = new RedisDatastoreSpringInitializer(config, grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE).collect() { GrailsClass cls -> cls.clazz })
        initializer.registerApplicationIfNotPresent = false
        initializer.setSecondaryDatastore( manager.hasGrailsPlugin("hibernate") || manager.hasGrailsPlugin("hibernate4")  )
        return initializer.getBeanDefinitions((BeanDefinitionRegistry)applicationContext)
    }

}
