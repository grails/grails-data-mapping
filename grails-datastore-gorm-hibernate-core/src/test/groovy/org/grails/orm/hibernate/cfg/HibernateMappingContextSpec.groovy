package org.grails.orm.hibernate.cfg

import grails.gorm.annotation.Entity
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.ValueGenerator
import spock.lang.Specification

/**
 * Created by graemerocher on 07/10/2016.
 */
class HibernateMappingContextSpec extends Specification {

    void "test entity with custom id generator"() {
        when:"A context is created"
        def mappingContext = new HibernateMappingContext()
        PersistentEntity entity = mappingContext.addPersistentEntity(CustomIdGeneratorEntity)

        then:"The mapping is correct"
        entity.mapping.identifier.generator == ValueGenerator.CUSTOM
    }
}

@Entity
class CustomIdGeneratorEntity {
    String name
    static mapping = {
        id(generator: "org.grails.orm.hibernate.cfg.MyUUIDGenerator", type: "uuid-binary")
    }
}
class MyUUIDGenerator {

}