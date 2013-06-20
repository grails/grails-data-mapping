package grails.gorm.tests

import grails.gorm.JpaEntity

/**
 * @author Graeme Rocher
 */
@JpaEntity
class Record {
    Long id
    String name
    Date dateCreated
    Date lastUpdated

    static mapping = {
        autoTimestamp false
    }
}
