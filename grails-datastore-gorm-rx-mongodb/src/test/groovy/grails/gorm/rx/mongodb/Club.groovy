package grails.gorm.rx.mongodb

import grails.gorm.annotation.Entity

/**
 * Created by graemerocher on 09/05/16.
 */
@Entity
class Club implements RxMongoEntity<Club> {
    String name
    Sport sport
}
