package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity

/**
 * Created by graemerocher on 11/05/16.
 */
@Entity
class Face implements RxMongoEntity<Face> {
    Long version
    Nose nose
    String name
    static hasOne = [nose: Nose]
}