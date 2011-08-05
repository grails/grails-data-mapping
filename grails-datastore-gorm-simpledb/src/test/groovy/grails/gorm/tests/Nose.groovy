package grails.gorm.tests

import grails.persistence.Entity

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 8/5/11
 * Time: 3:03 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
class Nose implements Serializable{
    String id
    boolean hasFreckles
    Face face
    static belongsTo = [face:Face]
}