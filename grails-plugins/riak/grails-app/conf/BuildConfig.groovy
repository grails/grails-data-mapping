grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {

    inherits "global"

    log "warn"

    repositories {
        mavenRepo "http://repo.grails.org/grails/core"
        grailsCentral()
    }

    dependencies {

		def datastoreVersion = "1.0.0.RELEASE"
	    def riakDatastoreVersion = "1.0.0.BUILD-SNAPSHOT"
	    
	    compile( 'org.springframework.data:spring-data-riak:1.0.0.M3' ) {
	        transitive = false
	    }
	    compile( 'org.springframework.data:spring-data-keyvalue-core:1.0.0.M3' ) {
	        transitive = false
	    }
	    
	    runtime("org.codehaus.jackson:jackson-core-asl:1.7.4") {
	        transitive = false
	    }	    
	    runtime("org.codehaus.jackson:jackson-mapper-asl:1.7.4"){
	        transitive = false
	    }	    	    
	    
	    compile("org.grails:grails-datastore-gorm-riak:$riakDatastoreVersion",
	            "org.grails:grails-datastore-riak:$riakDatastoreVersion") {
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
    }
}
