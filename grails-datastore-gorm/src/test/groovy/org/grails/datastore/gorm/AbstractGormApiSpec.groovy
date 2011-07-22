package org.grails.datastore.gorm

import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.MappingContext

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

class TestGormStaticApi<D> extends GormStaticApi<D> {

    def myNewMethod() {}

    @Override D create() { super.create() }

    TestGormStaticApi(Class<D> persistentClass) {
        super(persistentClass, [getMappingContext: { -> [getPersistentEntity: { String name -> }] as MappingContext } ] as Datastore, [])
    }
}
