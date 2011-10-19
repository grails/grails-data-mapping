grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {

    inherits("global") {
        excludes 'xml-apis', 'netty'
    }

    log "warn"

    repositories {
        mavenRepo "http://repo.grails.org/grails/core"
        grailsCentral()
    }

    dependencies {

		  String datastoreVersion = "1.0.0.RC1"

        compile("org.grails:grails-datastore-gorm-redis:$datastoreVersion",
                "org.grails:grails-datastore-gorm-plugin-support:$datastoreVersion",        
                "org.grails:grails-datastore-gorm:$datastoreVersion",
                "org.grails:grails-datastore-core:$datastoreVersion",
                "org.grails:grails-datastore-redis:$datastoreVersion",
                "org.grails:grails-datastore-web:$datastoreVersion") {
            transitive = false
        }

        test("org.grails:grails-datastore-gorm-test:$datastoreVersion",
             "org.grails:grails-datastore-simple:$datastoreVersion") {
            transitive = false
        }
    }

    plugins {
        build(":release:1.0.0.RC3") {
            export = false
        }

        compile ":redis:1.0.0.M8"
    }
}
