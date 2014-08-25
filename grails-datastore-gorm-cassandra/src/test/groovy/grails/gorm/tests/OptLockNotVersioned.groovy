package grails.gorm.tests

import grails.persistence.Entity

@Entity
class OptLockNotVersioned implements Serializable {
    UUID id
    Long version

    String name

    static mapping = {
        version false
    }
}