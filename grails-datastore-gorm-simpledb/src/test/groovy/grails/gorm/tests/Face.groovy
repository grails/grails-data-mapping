package grails.gorm.tests

import grails.persistence.Entity

@Entity
class Face implements Serializable {
    String id
    String name
    Nose nose
    static hasOne = [nose: Nose]
}
