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
		def jedisResolver = new org.apache.ivy.plugins.resolver.URLResolver(name:"jedis", settings:ivySettings)
		jedisResolver.addArtifactPattern("http://github.com/downloads/xetorthio/jedis/[module]-[revision].jar")
		resolver(jedisResolver)

		mavenRepo "http://maven.springframework.org/milestone"
		mavenRepo "http://snapshots.repository.codehaus.org"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

      compile( "org.grails:grails-datastore-gorm-redis:1.0.0.M1" ) {
        excludes "slf4j-simple"
      }
      compile( "org.springframework:spring-datastore-web:1.0.0.M1" ){
        excludes "slf4j-simple"
      }
      test( "org.grails:grails-datastore-gorm-test:1.0.0.M1"){
        excludes "slf4j-simple"
      }

    }
}
