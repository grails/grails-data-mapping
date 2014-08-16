package grails.gorm.tests

import grails.persistence.Entity

@Entity
class OptLockVersioned implements Serializable {
    UUID id
    Long version

    String name
}