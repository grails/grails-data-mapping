package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class Task implements Serializable {
    Task task
    String name

    static mapping = {
        name index:true
    }

    static hasMany = [tasks:Task]
}