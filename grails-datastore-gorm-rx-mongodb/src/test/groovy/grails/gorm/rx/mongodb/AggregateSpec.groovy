package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.CityState
import org.bson.types.ObjectId

class AggregateSpec extends RxGormSpec {

    void "Test aggregate method"() {
        given:"Some test data"
        CityState.saveAll(
                new CityState(city:"San Francisco", state: "CA", pop: 3000),
                new CityState(city:"LA", state: "CA", pop: 5000),
                new CityState(city:"Dallas", state: "TX", pop: 4000),
                new CityState(city:"Austin", state: "CA", pop: 15000),
                new CityState(city:"Sacramento", state: "CA", pop: 1000)

        ).toBlocking().first()

        when:"An aggregation query is executed"
        def results = CityState.aggregate([
                ['$match':[pop:['$gte':1200]]]
        ]).toList().toBlocking().first()

        then:"The results are correct"
        results.size() == 4
        !results.find { it.city == "Sacramento" }
    }

    @Override
    List getDomainClasses() {
        [CityState]
    }
}

