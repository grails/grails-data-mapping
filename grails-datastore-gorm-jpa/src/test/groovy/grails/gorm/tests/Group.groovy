package grails.gorm.tests

import grails.gorm.JpaEntity
import javax.persistence.Table

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 11/24/11
 * Time: 11:50 AM
 * To change this template use File | Settings | File Templates.
 */
@JpaEntity
class UniqueGroup implements Serializable{
    Long id
    String name
    static constraints = {
        name unique:true
    }
    static mapping = {
        table 'groups_table'
    }
}

@JpaEntity
class GroupWithin implements Serializable{
    Long id
    String name
    String org
    static constraints = {
        name unique:"org"
    }
}

