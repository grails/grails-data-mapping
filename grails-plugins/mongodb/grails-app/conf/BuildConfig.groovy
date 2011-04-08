grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {

    inherits( "global" ) {
		excludes 'xml-apis', 'netty'
	}

    log "warn"

	def version = "1.0.0.groovy-1.7-BUILD-SNAPSHOT"	
	def repo = version.endsWith("-SNAPSHOT") ? 'snapshot' : 'milestone'


    repositories {
        mavenCentral()
		grailsCentral()
        mavenRepo "http://maven.springframework.org/$repo"
    }

    dependencies {

        def excludes = {
            excludes "slf4j-simple", "persistence-api", "commons-logging", "jcl-over-slf4j", "slf4j-api", "jta"
            excludes "spring-core", "spring-beans", "spring-aop", "spring-asm","spring-webmvc","spring-tx", "spring-context", "spring-web", "log4j", "slf4j-log4j12"
        }
        compile("org.mongodb:mongo-java-driver:2.4")
        runtime("com.gmongo:gmongo:0.7", excludes)
        compile("org.grails:grails-datastore-gorm-mongo:$version",
				"org.grails:grails-datastore-gorm:$version",
				"org.springframework:spring-datastore-core:$version",
				"org.springframework:spring-datastore-mongo:$version",
				"org.springframework:spring-datastore-web:$version") {
			transitive = false
		}
        test("org.grails:grails-datastore-gorm-test:$version",
			 "org.springframework:spring-datastore-simple:$version"){
			transitive = false
		}
    }

    plugins {
        build( ":maven-publisher:0.7.5" ) {
			export = false
		}
    }
}
