package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class ChildEntity implements Serializable {
    String name

    static mapping = { name index:true }

    static belongsTo = [TestEntity]
}