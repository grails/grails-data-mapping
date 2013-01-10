package grails.gorm.tests

/**
 * Tests hilo simpledb id generator
 */
class SimpleDBHiloSpec extends GormDatastoreSpec {

    void "Test one"() {
        given:
            def entity = new PlantNumericIdValue(name: "Single").save(flush:true)

        when:
            def result = PlantNumericIdValue.get(entity.id)

        then:
            result != null
            Long.parseLong(result.id) > 0
            result.id == Long.toString(Long.parseLong(result.id))
    }

    void "Test multiple"() {
        given:
            def entities = []
            for (i in 1..10) {
                entities.add(new PlantNumericIdValue(name: "OneOfThem-"+i).save(flush:true))
            }

        expect:
            //make sure ids are monotonically increase
            long previous = 0
            entities.each { it ->
                long current = Long.parseLong(it.id)
                assert current > previous
                previous = current
            }
    }
}
