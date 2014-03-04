package grails.gorm.tests;

import grails.persistence.Entity;

import java.io.Serializable;
import java.util.UUID;

@Entity
class Simples implements Serializable {
    UUID id
    String name
}