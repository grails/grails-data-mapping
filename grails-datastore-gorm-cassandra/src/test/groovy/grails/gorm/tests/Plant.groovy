package grails.gorm.tests;

import grails.persistence.Entity;

import java.io.Serializable;
import java.util.UUID;

@Entity
class Plant implements Serializable {
    UUID id
    Long version
    boolean goesInPatch
    String name

    static mapping = {
        name index:true
        goesInPatch index:true
    }
}