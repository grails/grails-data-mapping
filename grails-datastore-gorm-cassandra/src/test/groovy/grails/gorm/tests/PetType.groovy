package grails.gorm.tests

import grails.gorm.CassandraEntity
import grails.persistence.Entity

@CassandraEntity
class PetType implements Serializable {
    private static final long serialVersionUID = 1
    UUID id
    Long version
    String name

    static belongsTo = Pet
}