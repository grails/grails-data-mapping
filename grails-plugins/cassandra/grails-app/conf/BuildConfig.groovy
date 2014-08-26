grails.project.work.dir = 'target'
grails.project.source.level = 1.6

grails.project.dependency.resolver="maven"
grails.project.dependency.resolution = {
    
    inherits( "global" ) {
        excludes 'xml-apis', 'netty'
    }
    log 'warn'

    repositories {
        mavenCentral()
        grailsCentral()
        mavenLocal()

    }

    dependencies {
        def excludes = {
            excludes "slf4j-simple", "persistence-api", "commons-logging", "jcl-over-slf4j", "slf4j-api", "jta"
            excludes "spring-core", "spring-beans", "spring-aop", "spring-asm","spring-webmvc","spring-tx", "spring-context", "spring-web", "log4j", "slf4j-log4j12"
            excludes group:"org.grails", name:'grails-core'
            excludes group:"org.grails", name:'grails-gorm'
            excludes group:"org.grails", name:'grails-test'
            excludes group:'xml-apis', name:'xml-apis'
            excludes 'ehcache-core'            
        }       

        def datastoreVersion = "3.1.2.RELEASE"
        def cassandraDatastoreVersion = "0.5.BUILD-SNAPSHOT"

        compile ("org.grails:grails-datastore-gorm-cassandra:$cassandraDatastoreVersion",excludes)

        compile("org.grails:grails-datastore-gorm-plugin-support:$datastoreVersion",
                "org.grails:grails-datastore-gorm:$datastoreVersion",
                "org.grails:grails-datastore-core:$datastoreVersion",                
                "org.grails:grails-datastore-simple:$datastoreVersion",    
                "org.grails:grails-datastore-web:$datastoreVersion",excludes)         
        
    }

    plugins {
        build(':release:3.0.1', ':rest-client-builder:2.0.0') {
            export = false
            excludes 'grails-core', 'grails-web'
        }
    }
}
