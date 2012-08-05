grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {

    inherits "global"

    log "warn"

    String simpledbVersion = "0.4"
    //for local development and testing of the plugin:
    // 1) change version in grails-data-mapping/build.gradle to an appropriate snapshot
    // 2) grails-data-mapping/gradle install
    // 3) specify the same snapshot version here in the line below after the comments
    // 4) in your grails app BuildConfig: grails.plugin.location.'simpledb' = "C:/Source/grails-data-mapping/grails-plugins/simpledb"
    // 5) in your grails app BuildConfig: enable mavenLocal() in repositories AND put it first in the list of repos
//    String simpledbVersion = "0.2.BUILD-SNAPSHOT"

    String datastoreVersion = "1.0.0.RELEASE"

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
        compile("org.grails:grails-datastore-gorm-simpledb:$simpledbVersion",
                 "org.grails:grails-datastore-gorm-plugin-support:$datastoreVersion",
                 "org.grails:grails-datastore-gorm:$datastoreVersion",
                 "org.grails:grails-datastore-core:$datastoreVersion",
                 "org.grails:grails-datastore-simpledb:$simpledbVersion",
                 "org.grails:grails-datastore-web:$datastoreVersion") {
             transitive = false
         }        

        runtime("stax:stax:1.2.0", excludes)
        runtime('com.amazonaws:aws-java-sdk:1.3.3')

        test("org.grails:grails-datastore-gorm-test:$datastoreVersion",
             "org.grails:grails-datastore-simple:$datastoreVersion") {
            transitive = false
        }
    }
    
    plugins {
        build( ":release:1.0.1" ) {
            exported = false
        }
    }
}
