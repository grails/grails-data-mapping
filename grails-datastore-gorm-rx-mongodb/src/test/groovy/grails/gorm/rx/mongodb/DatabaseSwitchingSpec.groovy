package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Club
import grails.gorm.rx.mongodb.domains.Player

class DatabaseSwitchingSpec extends RxGormSpec {

    void "Test switching database"() {
        setup:
        def secondaryDatabase = 'laliga'
        Club.withDatabase(secondaryDatabase).getDB().drop().toBlocking().first()


        when:"An entity is created in the default database"
        Club c = new Club(name: "Arsenal").save().toBlocking().first()


        then:"It is not present when a secondary database is used"
        Club.count().toBlocking().first() == 1
        Club.withDatabase(secondaryDatabase).count().toBlocking().first() == 0

        when:"An item is saved to a different database"
        Club.withDatabase(secondaryDatabase).saveAll(new Club(name: "Real Sociedad")).toBlocking().first()

        then:"The item is saved to the correct database"
        Club.count().toBlocking().first() == 1
        Club.withDatabase(secondaryDatabase).count().toBlocking().first() == 1

    }
    @Override
    List<Class> getDomainClasses() {
        [Club, Player]
    }
}
