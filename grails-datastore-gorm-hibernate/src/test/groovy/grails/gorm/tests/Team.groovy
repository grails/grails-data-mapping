package grails.gorm.tests

import grails.persistence.Entity

/**
 * Created by graemerocher on 31/01/16.
 */
@Entity
class Team {
    Club club
    String name
}
