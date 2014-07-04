package grails.gorm.tests

import grails.gorm.CassandraEntity

@CassandraEntity
class OptLockNotVersioned implements Serializable {
    UUID id
    Long version

    String name

    static mapping = {
        version false
    }
}