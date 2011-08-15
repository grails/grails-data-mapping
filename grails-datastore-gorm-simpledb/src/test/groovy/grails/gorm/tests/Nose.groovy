package grails.gorm.tests

import grails.persistence.Entity

@Entity
class Nose implements Serializable {
    String id
    boolean hasFreckles
    Face face
    static belongsTo = [face: Face]
}
