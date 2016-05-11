package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity

/**
 * Created by graemerocher on 06/05/16.
 */
@Entity
class Simple implements RxMongoEntity<Simple> {
    String name
}
