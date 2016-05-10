package grails.gorm.rx.mongodb

import grails.gorm.annotation.Entity

/**
 * Created by graemerocher on 09/05/16.
 */
@Entity
class Sport implements RxMongoEntity<Sport> {

    String name
    static hasMany = [clubs: Club]
}
