grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsCentral()
        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        //mavenCentral()
        mavenLocal()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }
    dependencies {
        def excludes = {
             excludes "slf4j-simple", "persistence-api", "commons-logging", "jcl-over-slf4j", "slf4j-api", "jta"
             excludes "spring-core", "spring-beans", "spring-aop", "spring-asm","spring-webmvc","spring-tx", "spring-context", "spring-web", "log4j", "slf4j-log4j12"
             excludes group:"org.grails", name:'grails-core'
             excludes group:"org.grails", name:'grails-gorm'
             excludes group:"org.grails", name:'grails-test'
             excludes group:'xml-apis', name:'xml-apis'
             excludes 'ehcache-core'
             transitive = false
         }


         def datastoreVersion = "1.0.0.RELEASE"
         def jpaDatastoreVersion = "1.0.0.M1"

         compile ("org.grails:grails-datastore-jpa:$jpaDatastoreVersion",
                  "org.grails:grails-datastore-gorm-jpa:$jpaDatastoreVersion",excludes)
         compile("org.grails:grails-datastore-gorm-plugin-support:$datastoreVersion",
                 "org.grails:grails-datastore-gorm:$datastoreVersion",
                 "org.grails:grails-datastore-core:$datastoreVersion",                
                 "org.grails:grails-datastore-web:$datastoreVersion",excludes)

         runtime 'javassist:javassist:3.12.0.GA'

         test("org.grails:grails-datastore-gorm-test:$datastoreVersion",
              "org.grails:grails-datastore-simple:$datastoreVersion", excludes)
    }

    plugins {
        build(":tomcat:$grailsVersion",
              ":release:1.0.1") {
            export = false
        }
    }
}
