package grails.gorm.tests;

import grails.persistence.Entity;

import java.util.Date;
import java.util.UUID;

@Entity
class Record {
    UUID id
    String name
    Date dateCreated
    Date lastUpdated

    static constraints = {
        dateCreated nullable:true
        lastUpdated nullable:true
    }
    static mapping = {
        autoTimestamp false
    }
}