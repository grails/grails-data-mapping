package grails.gorm.tests

import grails.gorm.CassandraEntity
import grails.persistence.Entity

@CassandraEntity
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