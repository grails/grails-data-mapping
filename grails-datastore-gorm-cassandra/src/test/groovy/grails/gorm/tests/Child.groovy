package grails.gorm.tests;

import grails.persistence.Entity;

import java.io.Serializable;
import java.util.UUID;

@Entity
class Child implements Serializable {
    private static final long serialVersionUID = 1
    UUID id
    String name
}