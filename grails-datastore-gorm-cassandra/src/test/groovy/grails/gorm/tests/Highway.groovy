package grails.gorm.tests;

import java.io.Serializable;
import java.util.UUID;

import grails.persistence.Entity

@Entity
class Highway implements Serializable {
    UUID id
    Long version
    Boolean bypassed
    String name

    static mapping = {
        bypassed index:true
        name index:true
    }
}