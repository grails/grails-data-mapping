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
 * Represents a polygon for use in Geo data models
 */
@CompileStatic
@EqualsAndHashCode
class Polygon implements Shape{

    final List<Point> points

    Polygon(Point x, Point y, Point z, Point...others) {
        Assert.notNull(x, "Point x is required")
        Assert.notNull(y, "Point y is required")
        Assert.notNull(z, "Point z is required")
        Assert.notNull(others, "Point others is required")

        def list = Arrays.asList(x, y, z)
        list.addAll others
        this.points = list
    }

    public List<List<Double>> asList() {
        points.collect() { Point p -> p.asList() }
    }
}
