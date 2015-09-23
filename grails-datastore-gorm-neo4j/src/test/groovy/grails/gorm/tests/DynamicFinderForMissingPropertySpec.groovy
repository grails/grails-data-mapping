package grails.gorm.tests

import org.codehaus.groovy.grails.exceptions.InvalidPropertyException

class DynamicFinderForMissingPropertySpec extends GormDatastoreSpec {

    void 'test invoking a dynamic finder for a non-existent property'() {
        when:
        TestEntity.findAllBySomeMissingProperty('42')

        then:
        thrown InvalidPropertyException
    }
}
