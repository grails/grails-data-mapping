package org.grails.datastore.gorm.neo4j

import spock.lang.Specification

class UniqueConstraintPatcherSpec extends Specification {

    def "test patching of uniqueconstraint"() {
        when:
        new UniqueConstraintPatcher().patch()

        then:
        notThrown AssertionError
    }

}
