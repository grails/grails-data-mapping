package grails.gorm.tests

import grails.persistence.Entity

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 8/5/11
 * Time: 3:02 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
class Face implements Serializable{
    String id
    String name
    Nose nose
    static hasOne = [nose:Nose]
}