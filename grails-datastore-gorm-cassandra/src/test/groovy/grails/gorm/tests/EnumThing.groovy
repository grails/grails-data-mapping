package grails.gorm.tests;

import grails.persistence.Entity;

import java.util.UUID;

@Entity
class EnumThing {
    UUID id
    Long version
    String name
    TestEnum en

    static mapping = {
        name index: true
        en index: true
    }
}