package grails.gorm.tests;

import grails.persistence.Entity;

import java.io.Serializable;
import java.util.UUID;

@Entity
class OptLockVersioned implements Serializable {
    UUID id
    Long version

    String name
}