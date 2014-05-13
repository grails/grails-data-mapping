package grails.gorm.tests

import grails.gorm.CassandraEntity
import grails.persistence.Entity

@CassandraEntity
class Pet implements Serializable {
    UUID id
    Long version
    String name
    Date birthDate = new Date()
    PetType type = new PetType(name:"Unknown")
    Person owner
    Integer age
    Face face

    static mapping = {
        name index:true
    }

    static constraints = {
        owner nullable:true
        age nullable: true
        face nullable:true
    }
}