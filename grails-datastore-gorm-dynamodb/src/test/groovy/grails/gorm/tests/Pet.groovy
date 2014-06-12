package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS DynamoDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
@Entity
class Pet implements Serializable {
    String id
    Long version

    String name
    Date birthDate = new Date()
    PetType type = new PetType(name: "Unknown")
    Person owner

    String toString() {
        "Pet{id='$id', name='$name', birthDate=$birthDate, type=$type, owner=$owner}"
    }

    static constraints = {
        owner nullable:true
    }
}
