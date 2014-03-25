package grails.gorm.tests

import grails.persistence.Entity;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Entity
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