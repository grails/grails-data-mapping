package grails.gorm.tests


import grails.gorm.CassandraEntity

@CassandraEntity
class Country extends Location {
    Integer population   
}