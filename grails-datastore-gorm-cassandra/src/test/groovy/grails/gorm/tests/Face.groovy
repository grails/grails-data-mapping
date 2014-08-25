package grails.gorm.tests

import grails.persistence.Entity

@Entity
class Face implements Serializable {
    UUID id
    Long version
    String name
    Nose nose
    Person person
    static hasOne = [nose: Nose]
    static belongsTo = [person:Person]

    static constraints = {
        person nullable:true
    }
}