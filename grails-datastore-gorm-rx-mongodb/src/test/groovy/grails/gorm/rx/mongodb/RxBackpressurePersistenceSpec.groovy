package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Simple
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import rx.Observable
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

class RxBackpressurePersistenceSpec extends Specification {

    void setup() {
//        def client = new RxMongoDatastoreClient("mongodb://192.168.99.100", "test", Simple)
        def client = new RxMongoDatastoreClient("test", Simple)
        client.dropDatabase()

        Simple s = Observable.range(0, 25000).flatMap {
            new Simple(name: "Fred $it").save(failOnError: true)
        }.toBlocking().last()

        assert s, "the entities wasn't persisted properly"
    }

    void "Test basic persistence of MongoDB using RX - plain with backpressure"() {
        when:"All Freds are listed"
        AtomicInteger count = new AtomicInteger(0)
        Simple.where {
            name ==~ ~/Fred.+/
        }
        .toObservable()
        .doOnNext { println "Now we have $it.name on the stage..."}
        // this is the ideal state - nothing fancy here and it works out of the box
        .subscribe {
            // emulates show processing
            Thread.sleep(10)
            count.incrementAndGet()
        }

        then:"The results are correct"
        count.get() == 25000
    }

    void "Test basic persistence of MongoDB using RX - on backpressure buffered"() {
        when:"All Freds are listed"
        AtomicInteger count = new AtomicInteger(0)
        Simple.where {
            name ==~ ~/Fred.+/
        }
        .toObservable()
        // this often causes OOME (if the underlying implementation wouldn't fail with too many connections)
        .onBackpressureBuffer()
        .doOnNext { println "Now we have $it.name on the stage..."}
        .subscribe {
            // emulates show processing
            Thread.sleep(10)
            count.incrementAndGet()
        }

        then:"The results are correct"
        count.get() == 25000
    }

    void "Test basic persistence of MongoDB using RX - buffered to prevent backpressure"() {
        when:"All Freds are listed"
        AtomicInteger count = new AtomicInteger(0)
        Simple.where {
            name ==~ ~/Fred.+/
        }
        .toObservable()
        .doOnNext { println "Now we have $it.name on the stage..."}
        // buffer requests from the observable by 50 each time which should prevent the backpressure of well-behaved
        // observables
        .buffer(50)
        .subscribe {
            // emulates show processing
            Thread.sleep(10)
            count.addAndGet(it.size())
        }

        then:"The results are correct"
        count.get() == 25000
    }
}
