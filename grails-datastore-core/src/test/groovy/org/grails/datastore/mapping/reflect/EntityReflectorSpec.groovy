package org.grails.datastore.mapping.reflect

import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import spock.lang.Specification

/**
 * Created by graemerocher on 08/12/16.
 */
class EntityReflectorSpec extends Specification {

    void "test retrieve and set a property from a trait"() {
        when:
        MappingContext mappingContext = new KeyValueMappingContext("test")
        PersistentEntity entity = mappingContext.addPersistentEntity(Bar)

        then:"the property from the trait can be reflected"
        entity.reflector.getPropertyReader('bar').read(new Bar(bar: "test")) == 'test'
    }
}

trait Foo {
    String bar
}

class Bar implements Foo {
    String name
}
