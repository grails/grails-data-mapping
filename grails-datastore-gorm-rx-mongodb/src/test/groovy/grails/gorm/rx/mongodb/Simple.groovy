package grails.gorm.rx.mongodb

import grails.gorm.annotation.Entity

/**
 * Created by graemerocher on 06/05/16.
 */
@Entity
class Simple implements RxMongoEntity<Simple> {
    String name
}
