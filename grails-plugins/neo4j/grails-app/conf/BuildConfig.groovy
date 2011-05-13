grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
//grails.project.war.file = "target/${appName}-${appVersion}.war"
grails.project.dependency.resolution = {
	// inherit Grails' default dependencies
	inherits("global") {
		// uncomment to disable ehcache
		// excludes 'ehcache'
	}
	log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
	repositories {
		grailsPlugins()
		grailsHome()
		grailsCentral()

		// uncomment the below to enable remote dependency resolution
		// from public Maven repositories
		mavenLocal()
//		mavenCentral()
//		mavenRepo "http://maven.springframework.org/snapshot"
//		mavenRepo "http://maven.springframework.org/milestone"
            //mavenRepo "http://snapshots.repository.codehaus.org"
            //mavenRepo "http://repository.codehaus.org"
            //mavenRepo "http://download.java.net/maven/2/"
            //mavenRepo "http://repository.jboss.com/maven2/"
	}
	dependencies {

        def version = "1.0.0.groovy-1.7-M5"

//		def excludes = {
//			excludes "slf4j-simple", "persistence-api", "commons-logging", "jcl-over-slf4j", "slf4j-api", "jta", "slf4j-log4j12"
//			excludes "spring-core", "spring-beans", "spring-aop", "spring-tx", "spring-context", "spring-web"
//        }

        compile("org.grails:grails-datastore-gorm-neo4j:$version",
                "org.grails:grails-datastore-gorm:$version",
                "org.springframework:spring-datastore-core:$version",
                "org.springframework:spring-datastore-web:$version") {
            transitive = false
        }

        test("org.grails:grails-datastore-gorm-test:$version",
             "org.springframework:spring-datastore-simple:$version"){
            transitive = false
        }

        compile('org.neo4j:neo4j:1.3')

        def neo4jRestExcludes = {
//            excludes "jersey-server"
//            excludes "jersey-client"
//            excludes  "jackson-jaxrs"
//            excludes  "jackson-mapper-asl"
            excludes  "lucene-core"
            excludes  "neo4j-lucene-index"
            excludes  "neo4j-kernel"
		}

        compile("org.neo4j:neo4j-rest-graphdb:0.1-SNAPSHOT", neo4jRestExcludes)

	}
}
