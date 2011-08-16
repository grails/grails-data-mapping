grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {

    inherits( "global" ) {
        excludes 'xml-apis', 'netty'
    }

    log "warn"

    repositories {
        mavenLocal()
        mavenCentral()
        grailsCentral()
        mavenRepo "http://repo.grails.org/grails/core"
    }

    dependencies {

        def excludes = {
            excludes "slf4j-simple", "persistence-api", "commons-logging", "jcl-over-slf4j", "slf4j-api", "jta"
            excludes "spring-core", "spring-beans", "spring-aop", "spring-asm","spring-webmvc","spring-tx", "spring-context", "spring-web", "log4j", "slf4j-log4j12"
        }

        compile("org.mongodb:mongo-java-driver:2.5.3")
        compile("org.springframework.data:spring-data-mongodb:1.0.0.M3", excludes)
        runtime("com.gmongo:gmongo:0.8", excludes)

        String datastoreVersion = "1.0.0.M8"

        compile("org.grails:grails-datastore-gorm-mongo:$datastoreVersion",
                "org.grails:grails-datastore-gorm-plugin-support:$datastoreVersion",
                "org.grails:grails-datastore-gorm:$datastoreVersion",
                "org.grails:grails-datastore-core:$datastoreVersion",
                "org.grails:grails-datastore-mongo:$datastoreVersion",
                "org.grails:grails-datastore-web:$datastoreVersion") {
            transitive = false
        }

        test("org.grails:grails-datastore-gorm-test:$datastoreVersion",
             "org.grails:grails-datastore-simple:$datastoreVersion") {
            transitive = false
        }
    }

    plugins {
        build( ":release:1.0.0.RC3" ) {
            export = false
        }
    }
}
