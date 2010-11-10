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

	 	mavenRepo 'http://maven.springframework.org/milestone'
        mavenRepo 'http://repository.codehaus.org'
        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        //mavenLocal()
        //mavenCentral()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.


	    def excludes = {
	        excludes "slf4j-simple", "persistence-api", "commons-logging", "jcl-over-slf4j", "slf4j-api", "jta"
	        excludes "spring-core", "spring-beans", "spring-aop", "spring-tx", "spring-context", "spring-web"
	    }
		compile("org.springframework.data:spring-datastore-mongodb:1.0.0.BUILD-SNAPSHOT")
		compile("com.gmongo:gmongo:0.5.1")
      	compile("org.grails:grails-datastore-gorm:1.0.0.BUILD-SNAPSHOT", excludes)	
      	compile("org.grails:grails-datastore-gorm-mongo:1.0.0.BUILD-SNAPSHOT", excludes)
      	compile( "org.springframework:spring-datastore-web:1.0.0.BUILD-SNAPSHOT", excludes)
      	test("org.grails:grails-datastore-gorm-test:1.0.0.BUILD-SNAPSHOT", excludes)
    }
}
