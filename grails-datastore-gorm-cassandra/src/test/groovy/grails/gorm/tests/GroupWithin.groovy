package grails.gorm.tests;

import grails.persistence.Entity;

import java.io.Serializable;
import java.util.UUID;

@Entity
class GroupWithin implements Serializable {
    UUID id
    String name
    String org
    static constraints = {
        name unique:"org", index:true
        org index:true
    }
}