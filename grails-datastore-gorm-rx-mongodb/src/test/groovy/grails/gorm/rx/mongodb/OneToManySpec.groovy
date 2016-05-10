package grails.gorm.rx.mongodb

import grails.gorm.rx.collection.RxPersistentCollection
import grails.gorm.rx.proxy.ObservableProxy

/**
 * Created by graemerocher on 09/05/16.
 */
class OneToManySpec extends RxGormSpec {

    void "test bidirectional one-to-many persistence"() {

        when:"A an entity with a bidirectional one-to-many is saved"
        def sport = new Sport(name: "Association Football")
                            .addTo('clubs', new Club(name: "Manchester United"))
                            .save().toBlocking().first()

        then:"The entity and the association has been saved"
        sport.id
        sport.clubs.size() == 1
        sport.clubs.iterator().next().id

        when:"The association is retrieved again"
        sport = Sport.get(sport.id).toBlocking().first()
        Club club = sport.clubs.iterator().next()

        then:"The association can be read"
        sport.name == "Association Football"
        sport.clubs instanceof RxPersistentCollection
        sport.clubs.size() == 1
        club.name == "Manchester United"
        club.id
        club.sport == sport


        when:"The inverse side is loaded"
        club = Club.get(club.id).toBlocking().first()

        then:"The state is correct"
        club.id
        club.name == 'Manchester United'

        club.sport instanceof ObservableProxy
        ((ObservableProxy)club.sport).toObservable().toBlocking().first()
        club.sport.name == "Association Football"
        club.sport.clubs.size() == 1
    }

    @Override
    List<Class> getDomainClasses() {
        [Sport, Club]
    }
}
