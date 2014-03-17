/* Copyright (C) 2014 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.gorm.mongo.geo

import com.mongodb.DBObject
import grails.mongodb.geo.Shape
import groovy.transform.CompileStatic
import org.bson.BSONObject
import org.bson.BasicBSONObject
import org.grails.datastore.mapping.engine.types.AbstractMappingAwareCustomTypeMarshaller
import org.grails.datastore.mapping.model.PersistentProperty

/**
 * Abstract class for persisting {@link Shape} instances in GeoJSON format
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@CompileStatic
abstract class GeoJSONType<T extends Shape> extends AbstractMappingAwareCustomTypeMarshaller<T, DBObject, DBObject> {


    public static final String COORDINATES = "coordinates"
    public static final String GEO_TYPE = "type"

    GeoJSONType(Class<T> targetType) {
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
            BSONObject pointData = (BSONObject)obj
            def coords = pointData.get(COORDINATES)

            if(coords instanceof List) {
                return createFromCoords(coords)
            }
        }
        return null
    }

    abstract T createFromCoords(List coords)
}
