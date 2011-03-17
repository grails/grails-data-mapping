package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class PetType implements Serializable {
    String name
}
