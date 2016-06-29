package grails.gorm.tests

import grails.persistence.Entity

/**
 * Created by graemerocher on 01/02/16.
 */
@Entity
class Contract {
    BigDecimal salary
    static belongsTo = [player:Player]
}
