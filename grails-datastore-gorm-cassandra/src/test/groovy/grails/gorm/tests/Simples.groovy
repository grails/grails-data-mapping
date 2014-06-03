package grails.gorm.tests;

import grails.gorm.CassandraEntity;
import grails.persistence.Entity;

import java.io.Serializable;
import java.util.UUID;

@CassandraEntity
class Simples implements Serializable {
    UUID id
    String name
}