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
        mavenCentral()
        grailsCentral()
    }

    dependencies {
        // need this here till the new redis plugin that we'll rely on is released, otherwise we can't package-plugin
        compile('redis.clients:jedis:2.0.0')

        def version = "1.0.0.M7"

        compile("org.grails:grails-datastore-gorm-redis:$version",
                "org.grails:grails-datastore-gorm:$version",
                "org.grails:grails-datastore-core:$version",
                "org.grails:grails-datastore-redis:$version",
                "org.grails:grails-datastore-web:$version") {
            transitive = false
        }

        test("org.grails:grails-datastore-gorm-test:$version",
             "org.grails:grails-datastore-simple:$version") {
            transitive = false
        }
    }

    plugins {
        build(":release:1.0.0.M2") {
            export = false
        }
    }
}
