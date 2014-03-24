package grails.gorm.tests;

import grails.persistence.Entity;

import java.util.Set;

@Entity
class Country extends Location {
    Integer population

    static hasMany = [residents:Person]
    Set residents
}