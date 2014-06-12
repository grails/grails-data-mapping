package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

@Entity
class City extends Location{
    String id
    Long version
    BigDecimal latitude
    BigDecimal longitude
}