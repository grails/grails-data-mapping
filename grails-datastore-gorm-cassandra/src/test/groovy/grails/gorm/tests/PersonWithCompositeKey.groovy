package grails.gorm.tests;

import grails.persistence.Entity;

import java.io.Serializable;

@Entity
class PersonWithCompositeKey implements Serializable {
    Long version
    String firstName
    String lastName
    Integer age
    static mapping = {
        id composite: ['lastName', 'firstName']
    }
}