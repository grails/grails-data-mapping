package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class PersonEvent implements Serializable {
    String name
    Date dateCreated
    Date lastUpdated

    static STORE = [updated:0, inserted:0]

    static void resetStore() {
        STORE = [updated:0, inserted:0]
    }

    void beforeDelete() {
        STORE["deleted"] = true
    }

    void beforeUpdate() {
        STORE["updated"]++
    }

    void beforeInsert() {
        STORE["inserted"]++
    }
}
