package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.LastUpdatedTracker

/**
 * Created by graemerocher on 12/05/16.
 */
class LastUpdatedSpec extends RxGormSpec {
    void "Test lastUpdated and dateCreated"() {
        when:"An object is saved"
        def lum = new LastUpdatedTracker(name: "Fred")
        lum.save(flush:true).toBlocking().first()
        lum = LastUpdatedTracker.get(lum.id).toBlocking().first()

        then:"The dateCreated and lastUpdated properties are populated"
        lum.dateCreated != null
        lum.lastUpdated != null

        when:"The object is updated"
        sleep 1000
        def previousLastUpdated = lum.lastUpdated
        def previousDateCreated = lum.dateCreated
        lum.name = "Bob"
        lum.save(flush:true).toBlocking().first()

        lum = LastUpdatedTracker.get(lum.id).toBlocking().first()

        then:"lastUpdated is updated but date created is the same"
        lum.lastUpdated != previousLastUpdated
        lum.lastUpdated > lum.dateCreated
        lum.dateCreated == previousDateCreated
    }

    @Override
    List getDomainClasses() {
        [LastUpdatedTracker]
    }
}
