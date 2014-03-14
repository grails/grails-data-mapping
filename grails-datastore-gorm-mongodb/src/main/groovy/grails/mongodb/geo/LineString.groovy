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
package grails.mongodb.geo

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode

/**
 * See http://geojson.org/geojson-spec.html#linestring
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@CompileStatic
@EqualsAndHashCode
class LineString implements Shape{
    List<Point>  coordinates

    LineString(Point...points) {
        if(points.size() < 2)
            throw new IllegalArgumentException("At least 2 points required for a LineString")
        this.coordinates = points.toList()
    }

    @Override
    List<List<Double>> asList() {
        coordinates.collect() { Point p -> p.asList()}
    }
}
