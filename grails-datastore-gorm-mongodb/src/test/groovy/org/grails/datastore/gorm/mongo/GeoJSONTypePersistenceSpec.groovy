package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.mongodb.geo.LineString
import grails.mongodb.geo.Point
import grails.mongodb.geo.Polygon
import grails.persistence.Entity

/**
 * Created by graemerocher on 17/03/14.
 */
class GeoJSONTypePersistenceSpec extends GormDatastoreSpec {


    void "Test persist GeoJSON types"() {
        given:"A domain with GeoJSON types"

            def point = new Point(5, 10)
            def poly = Polygon.valueOf([ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ])
            def line = LineString.valueOf([ [100.0, 0.0], [101.0, 1.0] ])
            def p = new Place(point: point, polygon: poly, lineString: line)

        when:"the entity is persisted and retrieved"
            p.save(flush:true)
            session.clear()
            p == Place.get(p.id)

        then:"The GeoJSON types are correctly loaded"
            p.point == point
            p.polygon == poly
            p.lineString == line
    }

    @Override
    List getDomainClasses() {
        [Place]
    }
}

@Entity
class Place {
    Long id
    Point point
    Polygon polygon
    LineString lineString
}
