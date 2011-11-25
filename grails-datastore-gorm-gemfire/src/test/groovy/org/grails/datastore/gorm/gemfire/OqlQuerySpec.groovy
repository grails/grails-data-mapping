package org.grails.datastore.gorm.gemfire

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Plant

/**
 * @author graemerocher
 */
class OqlQuerySpec extends GormDatastoreSpec{
    @Override
    List getDomainClasses() {
        [Plant]
    }


    void "test executeQuery method"() {
        given:
            new Plant(name:"rose", goesInPatch:false).save()
            new Plant(name:"oak", goesInPatch:false).save()
            new Plant(name:"daisy", goesInPatch:true).save(flush:true)

        when:
            def results = Plant.executeQuery("goesInPatch = false")

        then:
            results.size() == 2
    }

    void "test executeQuery method with positional parameters"() {
        given:
            new Plant(name:"rose", goesInPatch:false).save()
            new Plant(name:"oak", goesInPatch:false).save()
            new Plant(name:"daisy", goesInPatch:true).save(flush:true)

        when:
            def results = Plant.executeQuery('goesInPatch = $1', [false])

        then:
            results.size() == 2
    }

    void "test findAll method"() {
        given:
            new Plant(name:"rose", goesInPatch:false).save()
            new Plant(name:"oak", goesInPatch:false).save()
            new Plant(name:"daisy", goesInPatch:true).save(flush:true)

        when:
            def results = Plant.findAll("goesInPatch = false")

        then:
            results.size() == 2
    }

    void "test findAll method with arguments"() {
        given:
            new Plant(name:"rose", goesInPatch:false).save()
            new Plant(name:"oak", goesInPatch:false).save()
            new Plant(name:"daisy", goesInPatch:true).save(flush:true)

        when:
            def results = Plant.findAll("goesInPatch = false", [sort:'name']).sort { it.name }

        then:
            results.size() == 2
            results[0].name == 'oak'
            results[1].name == 'rose'

        when:
            results = Plant.findAll("goesInPatch = false", [sort:'name', order:"desc"]).sort { it.name }.reverse()

        then:
            results.size() == 2
            results[0].name == 'rose'
            results[1].name == 'oak'
    }

    void "test findAll method with positional parameters"() {
        given:
            new Plant(name:"rose", goesInPatch:false).save()
            new Plant(name:"oak", goesInPatch:false).save()
            new Plant(name:"daisy", goesInPatch:true).save(flush:true)

        when:
            def results = Plant.findAll('goesInPatch = $1', [false])

        then:
            results.size() == 2
    }

    void "test findAll method with positional parameters and arguments"() {
        given:
            new Plant(name:"rose", goesInPatch:false).save()
            new Plant(name:"oak", goesInPatch:false).save()
            new Plant(name:"daisy", goesInPatch:true).save(flush:true)

        when:
            def results = Plant.findAll('goesInPatch = $1', [false], [sort:'name']).sort { it.name }

        then:
            results.size() == 2
            results[0].name == 'oak'
            results[1].name == 'rose'
    }

    void "test find method"() {
        given:
            new Plant(name:"rose", goesInPatch:false).save()
            new Plant(name:"oak", goesInPatch:false).save()
            new Plant(name:"daisy", goesInPatch:true).save(flush:true)

        when:
            def result = Plant.find("name = 'rose'")

        then:
            result != null
            result.name == 'rose'
    }

    void "test find method with positional parameters"() {
        given:
            new Plant(name:"rose", goesInPatch:false).save()
            new Plant(name:"oak", goesInPatch:false).save()
            new Plant(name:"daisy", goesInPatch:true).save(flush:true)

        when:
            def result = Plant.find('name = $1', ['rose'])

        then:
            result != null
            result.name == 'rose'
    }
}
