package grails.gorm.tests

import grails.persistence.Entity

@Entity
class Country extends Location {
    Integer population   
}