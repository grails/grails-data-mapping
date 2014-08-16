package grails.gorm.tests

import grails.persistence.Entity

@Entity
class Nose implements Serializable {
    UUID id
    Long version
    boolean hasFreckles
    Face face
    static belongsTo = [face: Face]

    static mapping = {
        face index:true
    }
}