grails.project.work.dir = 'target'
grails.project.source.level = 1.6

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
            transitive = false
        }

        compile("org.mongodb:mongo-java-driver:2.11.3", excludes)
        compile("org.springframework.data:spring-data-mongodb:1.2.1.RELEASE", excludes)
        compile("org.springframework.data:spring-data-commons-core:1.4.1.RELEASE", excludes)
        runtime 'org.springframework.data:spring-data-commons:1.5.1.RELEASE'
        runtime("com.gmongo:gmongo:1.2", excludes)

        def datastoreVersion = "1.1.9.RELEASE"
        def mongoDatastoreVersion = "1.3.1.RELEASE"

        compile ("org.grails:grails-datastore-mongo:$mongoDatastoreVersion",
                 "org.grails:grails-datastore-gorm-mongo:$mongoDatastoreVersion",excludes)
        compile("org.grails:grails-datastore-gorm-plugin-support:$datastoreVersion",
                "org.grails:grails-datastore-gorm:$datastoreVersion",
                "org.grails:grails-datastore-core:$datastoreVersion",                
                "org.grails:grails-datastore-web:$datastoreVersion",excludes)
        
        runtime 'org.javassist:javassist:3.16.1-GA'

        test("org.grails:grails-datastore-gorm-test:$datastoreVersion",
             "org.grails:grails-datastore-simple:$datastoreVersion", excludes)
    }

    plugins {
        build(':release:2.2.0', ':rest-client-builder:1.0.3') {
            export = false
        }
    }
}
