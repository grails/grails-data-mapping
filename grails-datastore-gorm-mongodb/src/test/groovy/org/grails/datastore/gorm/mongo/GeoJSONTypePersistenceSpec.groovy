package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.mongodb.geo.Box
import grails.mongodb.geo.Circle
import grails.mongodb.geo.LineString
import grails.mongodb.geo.Metric
import grails.mongodb.geo.Point
import grails.mongodb.geo.Polygon
import grails.mongodb.geo.Sphere
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

    void "Test geoWithin dynamic finder"() {
        given:"A domain with GeoJSON types"
            def point = new Point(2, 1)
            // Order: bottom left, top left, top right, bottom right, bottom left
            def poly1 = Polygon.valueOf([ [0.0, 0.0], [3.0, 0.0], [3.0, 3.0], [0.0, 3.0], [0.0, 0.0] ])
            def poly2 = Polygon.valueOf([ [5.0, 5.0], [7.0, 5.0], [7.0, 7.0], [5.0, 7.0], [5.0, 5.0] ])
            def p = new Place(point: point)
            p.save(flush:true)

        expect:"A geoWithin query is executed to find a point within"
            Place.findByPointGeoWithin(poly1)
            !Place.findByPointGeoWithin(poly2)
            Place.findByPointGeoWithin( Box.valueOf( [[0.0d, 0.0d], [5.0d, 5.0d]] ) )
            !Place.findByPointGeoWithin( Box.valueOf( [[5.0d, 5.0d], [10.0d, 10.0d]] ) )
            Place.findByPointGeoWithin( Circle.valueOf( [[1.0d, 1.0d], 3.0d] ) )
            !Place.findByPointGeoWithin( Circle.valueOf( [[10.0d, 10.0d], 3.0d] ) )
            Place.findByPointGeoWithin( Sphere.valueOf( [[1.0d, 1.0d], 0.06]) )
            !Place.findByPointGeoWithin( Sphere.valueOf( [[10.0d, 10.0d], 0.06] ) )
            Place.findByPoint(point)
            Place.findByPointGeoWithin([ '$polygon': [ [0.0d, 0.0d], [3.0d, 0.0d], [3.0d, 3.0d], [0.0d, 3.0d], [0.0d, 0.0d] ] ])

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

    static mapping = {
        point geoIndex:'2dsphere'
    }
}
