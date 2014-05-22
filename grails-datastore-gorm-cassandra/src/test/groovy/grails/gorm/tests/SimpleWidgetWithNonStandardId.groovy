package grails.gorm.tests

import grails.gorm.CassandraEntity

@CassandraEntity
class SimpleWidgetWithNonStandardId implements Serializable {
    Long myIdentifier
    Long version
    String name
    static mapping = {
        id name: 'myIdentifier'
    }
}