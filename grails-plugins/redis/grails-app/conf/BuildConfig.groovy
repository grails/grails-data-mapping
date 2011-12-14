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

		def datastoreVersion = "1.0.0.RELEASE"
	    def redisDatastoreVersion = "1.0.0.M8"
	    
	    compile("org.grails:grails-datastore-gorm-redis:$redisDatastoreVersion",
	            "org.grails:grails-datastore-redis:$redisDatastoreVersion") {
	        transitive = false
	    }

        compile(
                "org.grails:grails-datastore-gorm-plugin-support:$datastoreVersion",        
                "org.grails:grails-datastore-gorm:$datastoreVersion",
                "org.grails:grails-datastore-core:$datastoreVersion",                
                "org.grails:grails-datastore-web:$datastoreVersion") {
            transitive = false
        }

        test("org.grails:grails-datastore-gorm-test:$datastoreVersion",
             "org.grails:grails-datastore-simple:$datastoreVersion") {
            transitive = false
        }
    }

    plugins {
        build(":release:1.0.0") {
            export = false
        }

        compile ":redis:1.1"
    }
}
