package grails.gorm.tests

import grails.gorm.CassandraEntity

@CassandraEntity
class City extends Location {
    BigDecimal latitude
    BigDecimal longitude
}