package grails.gorm.tests;

import grails.persistence.Entity;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

@Entity
class Parent implements Serializable {
    private static final long serialVersionUID = 1
    UUID id
    String name
    Set<Child> children = []
    static hasMany = [children: Child]
}