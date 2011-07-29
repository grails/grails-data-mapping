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
        mavenRepo "http://repo.grails.org/grails/repo"
    }

    plugins {
        build(":release:1.0.0.M2") {
            export = false
        }
    }

    dependencies {

        def version = "1.0.0.M7"

//        def excludes = {
//            excludes "slf4j-simple", "persistence-api", "commons-logging", "jcl-over-slf4j", "slf4j-api", "jta", "slf4j-log4j12"
//            excludes "spring-core", "spring-beans", "spring-aop", "spring-tx", "spring-context", "spring-web"
//        }

        compile("org.grails:grails-datastore-gorm-neo4j:$version",
                "org.grails:grails-datastore-gorm:$version",
                "org.grails:grails-datastore-core:$version",
                "org.grails:grails-datastore-web:$version") {
            transitive = false
        }

        test("org.grails:grails-datastore-gorm-test:$version",
             "org.grails:grails-datastore-simple:$version"){
            transitive = false
        }

        compile('org.neo4j:neo4j-community:1.4')

/*        def neo4jRestExcludes = {
//            excludes "jersey-server"
//            excludes "jersey-client"
//            excludes  "jackson-jaxrs"
//            excludes  "jackson-mapper-asl"
            excludes  "lucene-core"
            excludes  "neo4j-lucene-index"
            excludes  "neo4j-kernel"
        }

        compile("org.neo4j:neo4j-rest-graphdb:0.1-SNAPSHOT", neo4jRestExcludes) // excluded as of now since snapshot is not available via a m2 repo
*/
    }
}
