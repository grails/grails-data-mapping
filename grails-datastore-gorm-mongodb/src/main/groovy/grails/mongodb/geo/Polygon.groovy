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

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import org.springframework.util.Assert

/**
 * Represents a GeoJSON polygon for use in Geo data models.
 * See http://geojson.org/geojson-spec.html#polygon
 */
@CompileStatic
@EqualsAndHashCode
class Polygon implements Shape{

    final List<Point> points

    /**
     * Constructs a Polygon for the given {@link Point} instances
     *
     * @param x The x {@link Point}
     * @param y The y {@link Point}
     * @param z The z {@link Point}
     * @param others The remaining {@link Point} instances
     */
    Polygon(Point x, Point y, Point z, Point...others) {
        Assert.notNull(x, "Point x is required")
        Assert.notNull(y, "Point y is required")
        Assert.notNull(z, "Point z is required")
        Assert.notNull(others, "Point others is required")

        def list = []
        list.addAll Arrays.asList(x, y, z)
        list.addAll others
        this.points = list
    }

    /**
     * Converts the Polygon to a multi-dimensional list of coordinates.
     * Example: [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ]
     *
     * @return The list
     */
    public List<List<List<Double>>> asList() {
         [ points.collect() { Point p -> p.asList() } ]
    }

    /**
     * The inverse of {@link Polygon#asList()}, constructs a Polygon from a coordinate list
     *
     * @param coords The coordinate list
     * @return A Polygon
     */
    static Polygon valueOf(List coords) {
        Assert.notNull(coords, "Argument coords cannot be null")

        if(coords.size() == 1) {
            // possible subcoordinates
            def o = coords.get(0)
            if(o instanceof List) {
                List subCoords = (List)o
                coords = subCoords
            }
        }
        if(coords.size() < 4) throw new IllegalArgumentException("Coordinates should contain at least 4 entries for a Polygon")

        Point x = Point.getPointAtIndex(coords, 0)
        Point y = Point.getPointAtIndex(coords, 1)
        Point z = Point.getPointAtIndex(coords, 2)

        List<Point> remaining = (List<Point>)coords.subList(3, coords.size()).collect() {
            if(it instanceof Point) {
                return it
            }
            else if(it instanceof List) {
                return Point.valueOf((List<Double>)it)
            }
            throw new IllegalArgumentException("Invalid coordinates: $coords")
        }

        return new Polygon(x, y, z, remaining as Point[])
    }

}
