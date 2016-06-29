package grails.gorm.tests

import grails.persistence.Entity

/**
 * Created by graemerocher on 29/04/14.
 */
class CustomColumnReadWriteSpec extends GormDatastoreSpec{

    void "Test custom read and write mapping"() {
        when:"An entity with a custom read write is saved"
            Name name = new Name(name:"bob").save()
            session.clear()
            name = Name.get(name.id)

        then:"the custom read write were used"
            name.name == 'BOBBOB'
    }

    @Override
    List getDomainClasses() {
        [Name]
    }
}

@Entity
class Name {
    Long id
    Long version

    String name

    static mapping = {
        name write:'UPPER(?)', read:'REPEAT(name, 2)'
    }
}
