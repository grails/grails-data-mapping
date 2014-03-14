package org.grails.datastore.gorm.mongo.geo

import com.mongodb.DBObject
import grails.mongodb.geo.Point
import grails.mongodb.geo.Shape
import org.bson.BSONObject
import org.bson.BasicBSONObject
import org.grails.datastore.mapping.engine.types.AbstractMappingAwareCustomTypeMarshaller
import org.grails.datastore.mapping.model.PersistentProperty

/**
 * Created by graemerocher on 14/03/14.
 */
abstract class GeoType<T extends Shape> extends AbstractMappingAwareCustomTypeMarshaller<T, DBObject, DBObject> {


    public static final String COORDINATES = "coordinates"
    public static final String GEO_TYPE = "type"

    GeoType(Class<T> targetType) {
        super(targetType)
    }

    @Override
    protected Object writeInternal(PersistentProperty property, String key, T value, DBObject nativeTarget) {

        def pointData = new BasicBSONObject()
        pointData.put(GEO_TYPE, targetType.simpleName)
        pointData.put(COORDINATES, value.asList())
        nativeTarget.put(key, pointData)
        return pointData
    }

    @Override
    protected T readInternal(PersistentProperty property, String key, DBObject nativeSource) {
        def obj = nativeSource.get(key)
        if(obj instanceof BSONObject) {
            BSONObject pointData = obj
            def coords = pointData.get(COORDINATES)

            if(coords instanceof List) {
                return createFromCoords(coords)
            }
        }
        return null
    }

    abstract T createFromCoords(List coords)
}
