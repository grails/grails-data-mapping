package grails.gorm.tests

import grails.gorm.CassandraEntity
import grails.persistence.Entity

import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform

@ApplyDetachedCriteriaTransform
@groovy.transform.EqualsAndHashCode 
@CassandraEntity
class Person implements Serializable, Comparable<Person> {
    
    UUID id
    Long version
    String firstName
    String lastName
    Integer age = 0    

    static Person getByFirstNameAndLastNameAndAge(String firstName, String lastName, int age) {
        find( new Person(firstName: firstName, lastName: lastName, age: age) )
    }

    static mapping = {        
        firstName index:true
        lastName index:true
        age index:true
    }

    static constraints = {

    }

    @Override
    int compareTo(Person t) {
        age <=> t.age
    }
}