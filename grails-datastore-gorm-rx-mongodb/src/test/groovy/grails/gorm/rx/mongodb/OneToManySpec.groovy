package grails.gorm.rx.mongodb

import grails.gorm.rx.collection.RxPersistentCollection
import grails.gorm.rx.mongodb.domains.Animal
import grails.gorm.rx.mongodb.domains.Carrot
import grails.gorm.rx.mongodb.domains.Club
import grails.gorm.rx.mongodb.domains.Donkey
import grails.gorm.rx.mongodb.domains.Player
import grails.gorm.rx.mongodb.domains.Sport
import grails.gorm.rx.proxy.ObservableProxy
import org.grails.datastore.mapping.dirty.checking.DirtyCheckingSet
import org.grails.datastore.rx.collection.RxPersistentSet
import rx.Observable

/**
 * Created by graemerocher on 09/05/16.
 */
class OneToManySpec extends RxGormSpec {

    void "Test that a one-to-many with inheritances behaves correctly"() {
        given:"A one-to-many association inherited from a parent"
        Animal animal = new Animal().save().toBlocking().first()
        Donkey donkey = new Donkey(name: "Eeyore").save().toBlocking().first()
        Carrot.saveAll(
            new Carrot(leaves: 1, animal: animal),
            new Carrot(leaves: 2, animal: animal),
            new Carrot(leaves: 3, animal: donkey),
            new Carrot(leaves: 4, animal: donkey)
        ).toBlocking().first()

        when:"The association is loaded"
        animal = Animal.get(animal.id).toBlocking().first()
        donkey = Donkey.get(donkey.id).toBlocking().first()

        then:"The association is correctly loaded"
        donkey.carrots.size() == 2
        animal.carrots.size() == 2
    }

    void "test unidirectioal one-to-many persistence"() {
        when:"A new unidirectional association is created"
        def sport = new Sport(name: "Association Football")
                .addTo('clubs', new Club(name: "Manchester United")
                                        .addTo('players', [name:"Cantona"]))
                .save().toBlocking().first()

        Club club = Club.first().toBlocking().first()

        then:"The association is persisted correctly"
        club.name == 'Manchester United'
        club.players instanceof RxPersistentSet
        club.players.size() == 1
        club.players.iterator().next().name == 'Cantona'

        when:"The association is updated"

        club.addTo('players', new Player(name: 'Giggs'))
            .save().toBlocking().first()

        club = Club.first().toBlocking().first()

        then:"The association is persisted correctly"
        club.name == 'Manchester United'
        club.players.size() == 2
        club.players.find { Player p -> p.name == "Cantona"}
        club.players.find { Player p -> p.name == "Giggs"}

        when:"An object is removed"
        club.removeFrom('players', club.players.find { Player p -> p.name == "Cantona"} )
        club.save().toBlocking().first()

        club = Club.first().toBlocking().first()

        then:"The association is persisted correctly"
        club.name == 'Manchester United'
        club.players.size() == 1
        club.players.find { Player p -> p.name == "Giggs"}
    }

    void "Test unidirectional one-to-many join query"() {
        when:"A new unidirectional association is created"
        def sport = new Sport(name: "Association Football")
                .addTo('clubs', new Club(name: "Manchester United")
                .addTo('players', [name:"Cantona"]))
                .save().toBlocking().first()

        Club club = Club.first([fetch:[players:'join']]).toBlocking().first()

        then:"The association is persisted correctly"
        club.name == 'Manchester United'
        club.players instanceof DirtyCheckingSet
        club.players.size() == 1
        club.players.iterator().next().name == 'Cantona'
    }

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



    void "Test persist many-to-one side"() {
        when:"The single ended is persisted"
        def sport = new Sport(name: "Association Football").save().toBlocking().first()

        Club club = new Club(name: "Manchester United", sport: sport).save().toBlocking().first()

        then:"Both were saved"
        club.id
        sport.id
        Club.list().toBlocking().first().size() == 1
        Sport.list().toBlocking().first().size() == 1
        Club.count().toBlocking().first() == 1
        Sport.count().toBlocking().first() == 1

        when:"The association is retrieved again"
        sport = Sport.get(sport.id).toBlocking().first()
        club = sport.clubs.iterator().next()

        then:"The association can be read"
        sport.name == "Association Football"
        sport.clubs instanceof RxPersistentCollection
        sport.clubs.size() == 1
        club.name == "Manchester United"
        club.id
        club.sport == sport

    }

    void "Test update update one-to-many side"() {
        when:"The single ended is persisted"
        Sport sport = new Sport(name: "Association Football").save().toBlocking().first()

        sport.addTo('clubs', [name:"Manchester United"])
             .save().toBlocking().first()

        sport = Sport.get(sport.id).toBlocking().first()

        then:"The results are correct"
        sport.name == "Association Football"
        sport.clubs.size() == 1

        when:"Another association is added"
        sport.addTo('clubs', [name:"Chelsea"])
                .save().toBlocking().first()

        sport = Sport.get(sport.id).toBlocking().first()

        then:"The results are correct"
        sport.name == "Association Football"
        sport.clubs.size() == 2

    }

    void "Test fetch one side with join query"() {
        when:"The single ended is persisted"
        Sport sport = new Sport(name: "Association Football").save().toBlocking().first()

        Club club = new Club(name: "Manchester United", sport: sport).save().toBlocking().first()

        def query = Sport.where {
            name == 'Association Football'
        }.join('clubs')

        sport = query.get().toBlocking().first()

        then:"The association is loaded too"
        sport.name == 'Association Football'
        // the clubs association is an initialized association
        sport.clubs instanceof DirtyCheckingSet
        !(sport.clubs.iterator().next() instanceof ObservableProxy)
    }

    void "Test fetch many side with join query"() {
        when:"The single ended is persisted"
        def sport = new Sport(name: "Association Football").save().toBlocking().first()

        Club club = new Club(name: "Manchester United", sport: sport).save().toBlocking().first()

        def query = Club.where {
            name == 'Manchester United'
        }.join('sport')

        club = query.get().toBlocking().first()

        then:"The association is loaded too"
        club.sport
        !(club.sport instanceof ObservableProxy)
    }

    void "Test fetch many side with with manual join"() {
        when:"The single ended is persisted"
        Sport sport = new Sport(name: "Association Football").save().toBlocking().first()

        Club club = new Club(name: "Manchester United", sport: sport).save().toBlocking().first()

        def query = Club.where {
            name == 'Manchester United'
        }.find().switchMap { Object o ->
            Observable.zip( Observable.just(o), Sport.get(o.sportId)) { Object[] results -> results }
        }


        def results = query.toBlocking().first()
        club = results[0]
        sport = results[1]

        then:"The association is loaded too"
        club.sport
        !(sport instanceof ObservableProxy)
    }

    @Override
    List<Class> getDomainClasses() {
        [Sport, Club, Animal, Carrot, Donkey]
    }
}
