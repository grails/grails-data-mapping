package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity

/**
 * Created by graemerocher on 11/05/16.
 */
@Entity
class Address implements RxMongoEntity<Individual> {
    String postCode
    Individual individual
    Individual2 individual2

    static belongsTo = [Individual, Individual2]

    static constraints = {
        individual nullable: true
        individual2 nullable: true
    }
}

