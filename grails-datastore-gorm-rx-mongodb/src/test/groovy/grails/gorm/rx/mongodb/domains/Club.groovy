package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity

/**
 * Created by graemerocher on 09/05/16.
 */
@Entity
class Club implements RxMongoEntity<Club> {
    String name
    Sport sport
    Set<Player> players

    static hasMany = [players:Player]
}
