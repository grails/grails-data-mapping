package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class ModifyPerson implements Serializable {
    Long version

    String name

    void beforeInsert() {
        name = "Fred"
    }
}
