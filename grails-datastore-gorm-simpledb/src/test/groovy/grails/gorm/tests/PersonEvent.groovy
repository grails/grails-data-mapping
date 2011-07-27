package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

@Entity
class PersonEvent implements Serializable {
    String id
    String name
    Date dateCreated
    Date lastUpdated

    def personService

    static STORE_INITIAL = [
        beforeDelete: 0, afterDelete: 0,
        beforeUpdate: 0, afterUpdate: 0,
        beforeInsert: 0, afterInsert: 0,
        beforeLoad: 0, afterLoad: 0]

    static STORE = [:] + STORE_INITIAL

    static void resetStore() {
        STORE = [:] + STORE_INITIAL
    }

    def beforeDelete() {
        STORE.beforeDelete++
    }

    void afterDelete() {
        STORE.afterDelete++
    }

    def beforeUpdate() {
        STORE.beforeUpdate++
    }

    void afterUpdate() {
        STORE.afterUpdate++
    }

    def beforeInsert() {
        STORE.beforeInsert++
    }

    void afterInsert() {
        STORE.afterInsert++
    }

    void beforeLoad() {
        STORE.beforeLoad++
    }

    void afterLoad() {
        STORE.afterLoad++
    }

    static mapping = {
        domain 'PersonEvent'
    }
}