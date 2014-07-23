package grails.gorm.tests

import grails.gorm.CassandraEntity
import grails.persistence.Entity

@CassandraEntity
class Dog implements Serializable{
    UUID id
    int age
    int deathAge
    String name

    static mapping = {
        age index:true
        name index:true
    }
}