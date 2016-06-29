package grails.gorm.tests

import grails.persistence.Entity
import groovy.transform.ToString

/**
 * Created by graemerocher on 31/01/16.
 */
@Entity
@ToString(includes = 'name')
class Team {
    Club club
    String name
    static hasMany = [players: Player]
}
