package grails.gorm.tests

import grails.gorm.JpaEntity 
import java.math.BigDecimal;


@JpaEntity
class City extends Location {
	BigDecimal latitude
	BigDecimal longitude
}