package org.grails.datastore.gorm.neo4j

import groovy.sql.Sql
import groovyx.gpars.GParsPool
import org.apache.tomcat.jdbc.pool.DataSource
import org.apache.tomcat.jdbc.pool.PoolProperties
import spock.lang.Ignore
import spock.lang.Specification

/**
 * Created by stefan on 05.05.14.
 */
@Ignore("these tests are just a playground to understand cypher jdbc")
class CypherJdbcSpec extends Specification {

    def "understand using cypher over groovy sql"() {
        setup:
        def sql = Sql.newInstance('jdbc:p6spy:neo4j://localhost:7474/')

        // NB: groovy SQL class does magic stuff, so we need to quote labels and reltypes
        sql.eachRow("match (m:`Movie` {title:{1}}) return m", ['The Matrix']) {
            println "row $it"
        }
        expect:
        true
    }

    def "understand using named params"() {
        setup:
        def sql = Sql.newInstance('jdbc:neo4j://localhost:7474/')
        sql.eachRow([title:'The Matrix'], "match (m:`Movie` {title:$title}) return m.title") { //}, ['The Matrix']) {
            println "row $it"
        }
        expect:
        true
    }

    def "test pooled datasources with neo4j jdbc"() {
        setup:
        def datasource = new DataSource(new PoolProperties(

                url: 'jdbc:neo4j:mem',
//                url: 'jdbc:neo4j://localhost:7474/' ,
                driverClassName: 'org.neo4j.jdbc.Driver',
                defaultAutoCommit: false

        ) )

        when:
        GParsPool.withPool(1000) {
            (1..10000).eachParallel { counter ->

                def connection = datasource.getConnection()
                try {
                    println "connection: $connection"
                    def statement = connection.prepareStatement("create (:Parallel {number: {1}})")
                    statement.setObject(1, counter)
                    def rs = statement.executeQuery()
                    connection.commit()
                } finally {
                    connection.close()
                }
            }
        }

        then:
        true
    }

}
