grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {

    inherits "global"

    log "warn"

    String datastoreVersion = "1.0.0.M9"
//    String datastoreVersion = "1.0.0.BUILD-SNAPSHOT" //for local development of the plugin

    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenRepo "http://repo.grails.org/grails/core"
        mavenLocal()
        mavenCentral()
        mavenRepo 'http://repository.codehaus.org'
    }

    dependencies {

        def excludes = {
            transitive = false
        }
        compile("org.grails:grails-datastore-gorm-simpledb:$datastoreVersion",
                 "org.grails:grails-datastore-gorm-plugin-support:$datastoreVersion",
                 "org.grails:grails-datastore-gorm:$datastoreVersion",
                 "org.grails:grails-datastore-core:$datastoreVersion",
                 "org.grails:grails-datastore-simpledb:$datastoreVersion",
                 "org.grails:grails-datastore-web:$datastoreVersion") {
             transitive = false
         }        

        runtime("stax:stax:1.2.0", excludes)
        runtime('com.amazonaws:aws-java-sdk:1.2.0')

        test("org.grails:grails-datastore-gorm-test:$datastoreVersion",
             "org.grails:grails-datastore-simple:$datastoreVersion") {
            transitive = false
        }
    }
    
    plugins {
        build ":release:1.0.0.RC3", {
            exported = false
        }
    }
}
