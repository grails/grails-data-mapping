package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Plant

class SchemalessSpec extends RxGormSpec {

    @Override
    List<Class> getDomainClasses() {
        [Plant]
    }

    def "Test attach additional data"() {
        given:
        def p = new Plant(name:"Pineapple")
        p['color'] = "Yellow"
        p.save(flush:true).toBlocking().first()

        when:
        p = Plant.get(p.id).toBlocking().first()

        then:
        p.name == 'Pineapple'
        p['color'] == 'Yellow'

        when:
        p['hasLeaves'] = true
        p.save(flush:true).toBlocking().first()
        p = Plant.get(p.id).toBlocking().first()

        then:
        p.name == 'Pineapple'
        p['color'] == 'Yellow'
        p['hasLeaves'] == true

        when:"All objects are listed"
        def results = Plant.list().toBlocking().first()

        then:"The right data is returned and the schemaless properties accessible"
        results.size() == 1
        results[0].name == 'Pineapple'
        results[0]['color'] == 'Yellow'

        when:"A groovy finderAll method is executed"
        def newResults = results.findAll { it['color'] == 'Yellow' }

        then:"The embedded data is stil there"
        newResults.size() == 1
        newResults[0].name == 'Pineapple'
        newResults[0]['color'] == 'Yellow'

        when:"A dynamic finder is used on a schemaless property"
        def plant = Plant.findByColor("Yellow").toBlocking().first()

        then:"The dynamic finder works"
        plant.name == "Pineapple"

        when:"A criteria query is used on a schemaless property"
        plant = Plant.createCriteria().get {
            eq 'color', 'Yellow'
        }.toBlocking().first()

        then:"The criteria query works"
        plant.name == "Pineapple"
    }
}
