package org.grails.datastore.rx.mongodb.query.api

import grails.mongodb.geo.Distance
import grails.mongodb.geo.GeoJSON
import grails.mongodb.geo.Point
import grails.mongodb.geo.Shape
import org.grails.datastore.mapping.query.api.Criteria
/**
 * Mongo Criteria method
 *
 * @since 6.0
 * @author Graeme Rocher
 */
interface MongoCriteria extends Criteria {
    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria near(String property, List<?> value)

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria near(String property, List<?> value, Number maxDistance)

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria near(String property, List<?> value, Distance maxDistance)

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria near(String property, Point value)

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria near(String property, Point value, Number maxDistance)

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria near(String property, Point value, Distance maxDistance)

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria nearSphere(String property, List<?> value)

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria nearSphere(String property, List<?> value, Number maxDistance)

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria nearSphere(String property, List<?> value, Distance maxDistance)

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria nearSphere(String property, Point value)

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria nearSphere(String property, Point value, Number maxDistance)

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria nearSphere(String property, Point value, Distance maxDistance)
    /**
     * Geospacial query for values within a given box. A box is defined as a multi-dimensional list in the form
     *
     * [[40.73083, -73.99756], [40.741404,  -73.988135]]
     *
     * @param property The property
     * @param value A multi-dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria withinBox(String property, List<?> value)

    /**
     * Geospacial query for values within a given polygon. A polygon is defined as a multi-dimensional list in the form
     *
     * [[0, 0], [3, 6], [6, 0]]
     *
     * @param property The property
     * @param value A multi-dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria withinPolygon(String property, List<?> value)

    /**
     * Geospacial query for values within a given circle. A circle is defined as a multi-dimensial list containing the position of the center and the radius:
     *
     * [[50, 50], 10]
     *
     * @param property The property
     * @param value A multi-dimensional list of values
     * @return The criteria insstance
     */
    public MongoCriteria withinCircle(String property, List<?> value)

    /**
     * Geospacial query for the given shape returning records that are found within the given shape
     *
     * @param property The property
     * @param shape The shape
     * @return The criteria insstance
     */
    public MongoCriteria geoWithin(String property, Shape shape)


    /**
     * Geospacial query for the given shape returning records that are found to intersect the given shape
     *
     * @param property The property
     * @param shape The shape
     * @return The criteria insstance
     */
    public MongoCriteria geoIntersects(String property, GeoJSON shape)

    public MongoCriteria arguments(Map arguments)
}