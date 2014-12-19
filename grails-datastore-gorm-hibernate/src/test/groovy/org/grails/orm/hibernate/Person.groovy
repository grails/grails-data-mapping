package org.grails.orm.hibernate

import grails.persistence.Entity
import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform

@Entity
@ApplyDetachedCriteriaTransform
class Person {
    Long id
    Long version

    String firstName
    String lastName
    Integer age = 0
    Face face

    Set<Pet> pets
    static hasMany = [pets:Pet]
    static simpsons = where {
         lastName == "Simpson"
    }

    static constraints = {
        face nullable:true
    }

}

