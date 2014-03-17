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
 * Represents a point for use in Geo data models
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@EqualsAndHashCode
@CompileStatic
class Point implements Shape{
    double x, y

    Point(double x, double y) {
        this.x = x
        this.y = y
    }

    double[] asArray() { [x,y] as double[] }

    List<Double> asList() { [ x, y] }

    static Point fromList(List<Double> list) {
        if(list.size() != 2) throw new IllegalArgumentException("Invalid point data: $list")
        return new Point(list.get(0), list.get(1))
    }

    static Point valueOf(double x, double y) {
        new Point(x, y)
    }

    static Point valueOf(List<Double> coords) {
        if(coords.size() == 2) {
            def x = coords.get(0)
            def y = coords.get(1)
            if((x instanceof Double) && (y instanceof Double)) {
                return new Point(x, y)
            }
        }
        throw new IllegalArgumentException("Invalid coordinates: $coords")
    }

    static Point getPointAtIndex( List coords, int index ) {
        def coord = coords.get(index)
        if(coord instanceof Point) {
            return (Point)coord
        }
        else if(coord instanceof List) {
            return valueOf( (List<Double>) coord )
        }
        throw new IllegalArgumentException("Invalid coordinates: $coords")
    }

}
