package grails.gorm.tests

import org.grails.datastore.gorm.neo4j.IdGenerator
import org.grails.datastore.gorm.neo4j.SnowflakeIdGenerator
import spock.lang.Specification

/**
 * Created by stefan on 10.04.14.
 */
class SnowflakeIdGeneratorSpec extends Specification {

    def "snowflake returns different values"() {

        setup:
            def numberOfInvocations = 100000
            def ids = [] as Set

            IdGenerator generator = new SnowflakeIdGenerator()

        when:
            for (def i=0; i<numberOfInvocations; i++) {
                def id = generator.nextId()
                assert !ids.contains(id)
                ids << id
            }

        then:
            ids.size() == numberOfInvocations

    }
}
