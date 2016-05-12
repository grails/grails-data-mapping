package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity

/**
 * Created by graemerocher on 12/05/16.
 */
@Entity
class LastUpdatedTracker implements RxMongoEntity<LastUpdatedTracker>{

    String name
    Date dateCreated
    Date lastUpdated
}
