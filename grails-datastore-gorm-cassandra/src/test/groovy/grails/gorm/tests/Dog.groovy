package grails.gorm.tests;

import grails.persistence.Entity;

import java.io.Serializable;
import java.util.UUID;

@Entity
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