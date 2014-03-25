package grails.gorm.tests;

import grails.persistence.Entity;

import java.io.Serializable;

@Entity
class SimpleWidgetWithNonStandardId implements Serializable {
    Long myIdentifier
    Long version
    String name
    static mapping = {
        id name: 'myIdentifier'
    }
}