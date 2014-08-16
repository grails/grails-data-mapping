package grails.gorm.tests

import grails.persistence.Entity

@Entity
class Simples implements Serializable {
    UUID id
    String name
}