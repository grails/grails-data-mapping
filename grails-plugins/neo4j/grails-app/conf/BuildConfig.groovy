grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {

    inherits("global") {
        excludes 'xml-apis', 'netty'
    }
    log "warn"

    repositories {
        grailsPlugins()
        grailsHome()
        mavenLocal()   // for picking up self-built snapshots before fetching from grailsCentral
        grailsCentral()

        mavenCentral()
        mavenRepo "http://repo.grails.org/grails/repo"
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

        def datastoreVersion = "1.1.1.RELEASE"
        def neo4jDatastoreVersion = "1.0.0.BUILD-SNAPSHOT"
        //def neo4jDatastoreVersion = "1.0.0.M15"

        compile("org.grails:grails-datastore-gorm-neo4j:$neo4jDatastoreVersion",
                "org.grails:grails-datastore-gorm-plugin-support:$datastoreVersion",
                "org.grails:grails-datastore-gorm:$datastoreVersion",
                "org.grails:grails-datastore-core:$datastoreVersion",
                "org.grails:grails-datastore-web:$datastoreVersion", excludes)

        test("org.grails:grails-datastore-gorm-test:$datastoreVersion",
             "org.grails:grails-datastore-simple:$datastoreVersion", exlcudes)

        compile('org.neo4j:neo4j-community:1.8')
    }

    plugins {
        runtime ":jquery:1.7.1", {
            export = false
        }
        runtime ":resources:1.1.6", {
            export = false
        }
        runtime ":tomcat:$grailsVersion", {
            export = false
        }
        build ":release:2.0.4", {
            export = false
        }
        //runtime ":svn:1.0.2"
        test ":spock:0.6", {
            export = false
        }
    }

}
