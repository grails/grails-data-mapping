package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class Face implements Serializable {
    Long id
    String name
    Nose nose
    static hasOne = [nose: Nose]
}
