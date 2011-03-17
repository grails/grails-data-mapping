package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class Pet implements Serializable {
    String name
    Date birthDate = new Date()
    PetType type
    Person owner
}
