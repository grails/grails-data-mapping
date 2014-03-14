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

/**
 * Represents a point for use in Geo data models
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@Canonical
@CompileStatic
class Point implements Shape{
    double x, y

    double[] asArray() { [x,y] as double[] }

    List<Double> asList() { [ x, y] }

    static Point fromList(List<Double> list) {
        if(list.size() != 2) throw new IllegalArgumentException("Invalid point data: $list")
        return new Point(list.get(0), list.get(1))
    }
}
