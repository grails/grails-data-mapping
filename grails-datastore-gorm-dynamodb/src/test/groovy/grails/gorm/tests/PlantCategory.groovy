package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS DynamoDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
@Entity
class PlantCategory implements Serializable {
    String id
    Long version

    Set plants
    String name

    static hasMany = [plants:Plant]
}