package grails.gorm.tests;

import grails.persistence.Entity;

import java.io.Serializable;
import java.util.UUID;

@Entity
class UniqueGroup implements Serializable {
    UUID id
    String name
    static constraints = {
        name unique:true, index:true
    }
}