package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity

@Entity
class LongAddress extends Address {
    String firstLine
    String city

}
