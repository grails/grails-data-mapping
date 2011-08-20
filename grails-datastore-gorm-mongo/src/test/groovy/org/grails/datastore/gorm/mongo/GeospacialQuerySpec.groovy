package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity

class GeospacialQuerySpec extends GormDatastoreSpec {

    static {
        GormDatastoreSpec.TEST_CLASSES << Hotel
        GormDatastoreSpec.TEST_CLASSES << Hotel2
    }

    void "Test that we can query within a circle"() {
        given:
            new Hotel(name:"Hilton", location:[50.1, 50]).save()
            new Hotel(name:"Raddison", location:[150, 130]).save(flush:true)
            session.clear()

        when:
            def h = Hotel.findByLocation([50.1, 50])

        then:
            h != null

        when:
            h = Hotel.findByLocationWithinCircle([[40, 30], 40])

        then:
            h != null
            h.name == "Hilton"
        when:
            h = Hotel.findByLocationWithinCircle([[10, 10],30])

        then:
            h == null
    }

    void "Test that we can query within a box"() {
        given:
            new Hotel(name:"Hilton", location:[50, 50]).save()
            new Hotel(name:"Raddison", location:[150, 130]).save(flush:true)
            session.clear()

        when:
            def h = Hotel.findByLocation([50, 50])

        then:
            h != null

        when:
            h = Hotel.findByLocationWithinBox([[40, 30],[60, 70]])

        then:
            h != null
            h.name == "Hilton"
        when:
            h = Hotel.findByLocationWithinBox([[20, 10],[40, 30]])

        then:
            h == null
    }

    void "Test that we can query for nearby location"() {
        given:
            new Hotel(name:"Hilton", location:[50, 50]).save()
            new Hotel(name:"Raddison", location:[150, 130]).save(flush:true)
            session.clear()

        when:
            def h = Hotel.findByLocation([50, 50])

        then:
            h != null

        when:
            h = Hotel.findByLocationNear([50, 60])

        then:
            h != null
            h.name == "Hilton"
        when:
            h = Hotel.findByLocationNear([170, 160])

        then:
            h != null
            h.name == "Raddison"
    }

    void "Test that we can query for nearby location with criteria"() {
        given:
            new Hotel(name:"Hilton", location:[50, 50]).save()
            new Hotel(name:"Raddison", location:[150, 130]).save(flush:true)
            session.clear()

        when:
            def h = Hotel.findByLocation([50, 50])

        then:
            h != null

        when:
            h = Hotel.createCriteria().get { near( "location", [50, 60] ) }

        then:
            h != null
            h.name == "Hilton"
        when:
            h = Hotel.createCriteria().get { near( "location", [170, 160] ) }

        then:
            h != null
            h.name == "Raddison"
    }

    void "Test Map syntax"() {
        given:
            new Hotel2(name:"Hilton", location:[lat: 50.1, lon: 50]).save()
            new Hotel2(name:"Raddison", location:[lat: 150, lon: 130]).save(flush:true)
            session.clear()
            def h

//        when:
//            h = Hotel2.findByLocation([lat: 50.1, lon: 50])
//
//        then:
//            h != null

        when:
            h = Hotel2.findByLocationWithinCircle([[40, 30], 40])

        then:
            h != null
            h.name == "Hilton"

        when:
            h = Hotel2.findByLocationWithinCircle([[10, 10], 30])

        then:
            h == null
    }
}

@Entity
class Hotel {
    Long id
    String name
    List location

    static mapping = {
        location geoIndex:true
    }
}

@Entity
class Hotel2 {
    Long id
    String name
    Map location

    static mapping = {
        location geoIndex:true
    }
}
