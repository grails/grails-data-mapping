package grails.gorm.tests

import grails.persistence.Entity

@Entity
class PetType implements Serializable {
    private static final long serialVersionUID = 1
    UUID id
    Long version
    String name

    static belongsTo = Pet
}