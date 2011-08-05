package grails.gorm.tests

import grails.gorm.JpaEntity

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 8/5/11
 * Time: 3:02 PM
 * To change this template use File | Settings | File Templates.
 */
@JpaEntity
class Nose implements Serializable{
    Long id
    boolean hasFreckles
    Face face
    static belongsTo = [face:Face]
}