grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {
    inherits "global"
    log "warn"
    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()
        mavenRepo "http://repo.grails.org/grails/repo"
    }

    plugins {
        build(":release:1.0.0.RC3") {
            export = false
        }
    }

    dependencies {

        def version = "1.0.0.BUILD-SNAPSHOT"

        compile("org.grails:grails-datastore-gorm-neo4j:$version",
                "org.grails:grails-datastore-gorm-plugin-support:$version",        
                "org.grails:grails-datastore-gorm:$version",
                "org.grails:grails-datastore-core:$version",
                "org.grails:grails-datastore-web:$version") {
            transitive = false
        }

        test("org.grails:grails-datastore-gorm-test:$version",
             "org.grails:grails-datastore-simple:$version"){
            transitive = false
        }

        compile('org.neo4j:neo4j-community:1.6')
    }
}
