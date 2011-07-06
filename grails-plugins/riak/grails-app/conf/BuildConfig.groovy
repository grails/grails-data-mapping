grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {

    inherits "global"

    log "warn"

    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()
        mavenRepo 'http://maven.springframework.org/milestone'
        mavenRepo 'http://maven.springframework.org/snapshot'
        mavenRepo 'http://repository.codehaus.org'
    }

    dependencies {

        def excludes = {
            excludes "slf4j-simple", "persistence-api", "commons-logging", "jcl-over-slf4j", "slf4j-api", "jta"
            excludes "spring-core", "spring-beans", "spring-aop", "spring-asm", "spring-webmvc", "spring-tx", "spring-context", "spring-web", "log4j", "slf4j-simple"
        }
        runtime("org.springframework.data:spring-data-riak:1.0.0.M2", excludes)
        runtime("org.grails:grails-datastore-gorm:1.0.0.M4", excludes)
        runtime("org.grails:grails-datastore-gorm-riak:1.0.0.M4", excludes)
        runtime("org.springframework:spring-datastore-web:1.0.0.M4", excludes)
        runtime("org.springframework:spring-datastore-riak:1.0.0.M4", excludes)

        test("org.grails:grails-datastore-gorm-test:1.0.0.M4", excludes)
    }
}
