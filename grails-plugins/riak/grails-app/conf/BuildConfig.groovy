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
    mavenCentral()
    mavenRepo 'http://maven.springframework.org/milestone'
    mavenRepo 'http://maven.springframework.org/snapshot'
    mavenRepo 'http://repository.codehaus.org'
  }

  dependencies {
    // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

    def excludes = {
      excludes "slf4j-simple", "persistence-api", "commons-logging", "jcl-over-slf4j", "slf4j-api", "jta"
      excludes "spring-core", "spring-beans", "spring-aop", "spring-asm", "spring-webmvc", "spring-tx", "spring-context", "spring-web", "log4j", "slf4j-log4j12"
    }
    runtime("org.springframework.data:spring-data-riak:1.0.0.M1", excludes)
    runtime("org.grails:grails-datastore-gorm:1.0.0.M3", excludes)
    runtime("org.grails:grails-datastore-gorm-riak:1.0.0.M3", excludes)
    runtime("org.springframework:spring-datastore-web:1.0.0.M3", excludes)

    test("org.grails:grails-datastore-gorm-test:1.0.0.M3", excludes)

  }
}
