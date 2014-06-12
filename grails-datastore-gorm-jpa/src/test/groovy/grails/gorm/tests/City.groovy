package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class City extends Location {
    BigDecimal latitude
    BigDecimal longitude
}