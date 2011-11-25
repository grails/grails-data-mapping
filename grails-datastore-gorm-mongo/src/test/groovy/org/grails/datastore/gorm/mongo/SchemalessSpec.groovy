package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Plant

class SchemalessSpec extends GormDatastoreSpec{
    @Override
    List getDomainClasses() {
        [Plant]
    }


    def "Test attach additional data"() {
        given:
            def p = new Plant(name:"Pineapple")
            p.dbo.color = "Yellow"
            p.save(flush:true)
            session.clear()

        when:
            p = Plant.get(p.id)

        then:
            p.name == 'Pineapple'
            p.dbo.color == 'Yellow'
            p['color'] == 'Yellow'

        when:
            p['hasLeaves'] = true
            p.save(flush:true)
            session.clear()
            p = Plant.get(p.id)

        then:
            p.name == 'Pineapple'
            p.dbo.color == 'Yellow'
            p['color'] == 'Yellow'
            p['hasLeaves'] == true

        when:"All objects are listed"
            session.clear()
            def results = Plant.list()

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

    }
}
