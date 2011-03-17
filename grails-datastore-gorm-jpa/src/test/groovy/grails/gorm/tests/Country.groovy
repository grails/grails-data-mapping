package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class Country extends Location {
    Integer population

    static hasMany = [residents:Person]
}
