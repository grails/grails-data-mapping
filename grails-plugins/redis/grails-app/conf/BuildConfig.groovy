grails.project.work.dir = 'target'
grails.project.source.level = 1.6

grails.project.dependency.resolver="maven"
grails.project.dependency.resolution = {

    inherits("global") {
        excludes 'xml-apis', 'netty'
    }
    log 'warn'

    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        def excludes = {
            excludes "slf4j-simple", "persistence-api", "commons-logging", "jcl-over-slf4j", "slf4j-api", "jta"
            excludes "spring-core", "spring-beans", "spring-aop", "spring-asm","spring-webmvc","spring-tx", "spring-context", "spring-web", "log4j", "slf4j-log4j12"
            excludes group:"org.grails", name:'grails-core'
            excludes group:"org.grails", name:'grails-gorm'
            excludes group:"org.grails", name:'grails-test'
            transitive = false
        }

        def datastoreVersion = "3.1.1.RELEASE"
        def redisDatastoreVersion = "1.0.0.RELEASE"

        compile("org.grails:grails-datastore-gorm-redis:$redisDatastoreVersion") {
            exclude group:"org.grails", name:'grails-core'
            exclude group:"org.grails", name:'grails-bootstrap'
            exclude group:"org.grails", name:'grails-test'
            exclude group:"org.springframework", name:'spring-tx'
            exclude group:"org.springframework", name:'spring-core'
            exclude group:"org.springframework", name:'spring-beans'
        }
    }

    plugins {
        build(':release:3.0.1', ':rest-client-builder:2.0.1') {
            export = false
        }
    }
}
