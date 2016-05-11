package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity

/**
 * Created by graemerocher on 09/05/16.
 */
@Entity
class Sport implements RxMongoEntity<Sport> {

    String name
    static hasMany = [clubs: Club]
}
