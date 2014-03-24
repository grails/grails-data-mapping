package grails.gorm.tests;

import grails.persistence.Entity;

import java.math.BigDecimal;

@Entity
class City extends Location {
    BigDecimal latitude
    BigDecimal longitude
}