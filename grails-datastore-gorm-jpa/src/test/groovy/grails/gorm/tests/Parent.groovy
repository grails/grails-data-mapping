package grails.gorm.tests

import grails.gorm.JpaEntity

/**
 * @author Graeme Rocher
 */
@JpaEntity
class Parent {
    String name
    Set<Child> children = []
    static hasMany = [children: Child]
}
