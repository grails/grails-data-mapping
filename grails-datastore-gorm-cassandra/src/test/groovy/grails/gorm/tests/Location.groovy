package grails.gorm.tests

import grails.persistence.Entity;

import java.io.Serializable;
import java.util.UUID;

@Entity
class Location implements Serializable {
    UUID id
    Long version
    String name
    String code = "DEFAULT"

    def namedAndCode() {
        "$name - $code"
    }

    static mapping = {
        name index:true
        code index:true
    }
}
