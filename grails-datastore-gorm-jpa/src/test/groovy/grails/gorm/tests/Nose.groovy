package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class Nose implements Serializable {
    Long id
    boolean hasFreckles
    Face face
    static belongsTo = [face: Face]
}
