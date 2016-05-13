package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import grails.mongodb.geo.Box
import grails.mongodb.geo.Circle
import grails.mongodb.geo.GeometryCollection
import grails.mongodb.geo.LineString
import grails.mongodb.geo.MultiLineString
import grails.mongodb.geo.MultiPoint
import grails.mongodb.geo.MultiPolygon
import grails.mongodb.geo.Point
import grails.mongodb.geo.Polygon
import grails.mongodb.geo.Sphere
import org.bson.types.ObjectId

/**
 * Created by graemerocher on 11/05/16.
 */
@Entity
class Place implements RxMongoEntity<Place> {
    ObjectId id
    String name
    Point point
    Polygon polygon
    LineString lineString
    Box box
    Circle circle
    Sphere sphere
    MultiPoint multiPoint
    MultiLineString multiLineString
    MultiPolygon multiPolygon
    GeometryCollection geometryCollection

    static mapping = {
        point geoIndex:'2dsphere'
    }
}
