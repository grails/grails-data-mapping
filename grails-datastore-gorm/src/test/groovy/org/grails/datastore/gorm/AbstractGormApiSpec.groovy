package org.grails.datastore.gorm

import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.model.MappingContext

import spock.lang.Specification

/**
 * @author graemerocher
 */
class AbstractGormApiSpec extends Specification {

    void "Test extended GORM API methods"() {
        when:
            def api = new TestGormStaticApi(AbstractGormApiSpec)

        then:
            api.extendedMethods.size() == 1
            api.extendedMethods[0].name == 'myNewMethod'
    }
}

class TestGormStaticApi extends GormStaticApi {

    def myNewMethod() {}

    @Override def create() {
        return super.create()
    }



    TestGormStaticApi(Class persistentClass) {
        super(persistentClass, [getMappingContext:{-> [getPersistentEntity: { String name->}] as MappingContext }] as Datastore)
    }
}
