package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity

@Entity
class City extends Location implements RxMongoEntity<City> {
    BigDecimal latitude
    BigDecimal longitude
}