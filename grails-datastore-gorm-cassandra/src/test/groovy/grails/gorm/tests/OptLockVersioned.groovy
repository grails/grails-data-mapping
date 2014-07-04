package grails.gorm.tests


import grails.gorm.CassandraEntity

@CassandraEntity
class OptLockVersioned implements Serializable {
    UUID id
    Long version

    String name
}