package grails.gorm.tests

import grails.gorm.JpaEntity

import javax.persistence.Transient

@JpaEntity
class PersonEvent implements Serializable {
    String name
    Date dateCreated
    Date lastUpdated

    @Transient
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

    void beforeDelete() {
        STORE.beforeDelete++
    }

    void afterDelete() {
        STORE.afterDelete++
    }

    void beforeUpdate() {
        STORE.beforeUpdate++
    }

    void afterUpdate() {
        STORE.afterUpdate++
    }

    void beforeInsert() {
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
}
