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

        def excludes = {
            excludes "slf4j-simple", "persistence-api", "commons-logging", "jcl-over-slf4j", "slf4j-api", "jta"
            excludes "spring-core", "spring-beans", "spring-aop", "spring-asm","spring-webmvc","spring-tx", "spring-context", "spring-web", "log4j", "slf4j-log4j12"
            excludes group:"org.grails", name:'grails-core'
            excludes group:"org.grails", name:'grails-gorm'
            excludes group:"org.grails", name:'grails-test'
            transitive = false
        }
        
		def datastoreVersion = "1.0.0.RELEASE"
	    def riakDatastoreVersion = "1.0.0.BUILD-SNAPSHOT"
	    
	    compile( 'org.springframework.data:spring-data-riak:1.0.0.M3',excludes)
	    compile( 'org.springframework.data:spring-data-keyvalue-core:1.0.0.M3',excludes )
	    
	    runtime("org.codehaus.jackson:jackson-core-asl:1.7.4",excludes)
	    runtime("org.codehaus.jackson:jackson-mapper-asl:1.7.4",excludes)
	    
	    compile("org.grails:grails-datastore-gorm-riak:$riakDatastoreVersion",
	            "org.grails:grails-datastore-riak:$riakDatastoreVersion",excludes)

        compile(
                "org.grails:grails-datastore-gorm-plugin-support:$datastoreVersion",        
                "org.grails:grails-datastore-gorm:$datastoreVersion",
                "org.grails:grails-datastore-core:$datastoreVersion",                
                "org.grails:grails-datastore-web:$datastoreVersion",excludes)

        test("org.grails:grails-datastore-gorm-test:$datastoreVersion",
             "org.grails:grails-datastore-simple:$datastoreVersion",excludes)
    }

    plugins {
        build(":release:1.0.0") {
            export = false
        }
    }
}
