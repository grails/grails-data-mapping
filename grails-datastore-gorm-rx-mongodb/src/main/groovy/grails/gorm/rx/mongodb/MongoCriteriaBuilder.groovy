package grails.gorm.rx.mongodb

import grails.gorm.rx.CriteriaBuilder
import grails.mongodb.geo.Distance
import grails.mongodb.geo.GeoJSON
import grails.mongodb.geo.Point
import grails.mongodb.geo.Shape
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.grails.datastore.mapping.mongo.query.MongoQuery
import org.grails.datastore.mapping.query.api.QueryArgumentsAware
import org.grails.datastore.rx.mongodb.query.api.MongoCriteria

/**
 * An implementation of {@link CriteriaBuilder} for MongoDB
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
@InheritConstructors
class MongoCriteriaBuilder<T> extends CriteriaBuilder<T> implements MongoCriteria {

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria near(String property, List<?> value) {
        validatePropertyName(property, "near");
        addToCriteria(new MongoQuery.Near(property, value));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria near(String property, List<?> value, Number maxDistance) {
        validatePropertyName(property, "near");
        addToCriteria(new MongoQuery.Near(property, value, maxDistance));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria near(String property, List<?> value, Distance maxDistance) {
        validatePropertyName(property, "near");
        addToCriteria(new MongoQuery.Near(property, value, maxDistance));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria near(String property, Point value) {
        validatePropertyName(property, "near");
        addToCriteria(new MongoQuery.Near(property, value));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria near(String property, Point value, Number maxDistance) {
        validatePropertyName(property, "near");
        addToCriteria(new MongoQuery.Near(property, value, maxDistance));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria near(String property, Point value, Distance maxDistance) {
        validatePropertyName(property, "near");
        addToCriteria(new MongoQuery.Near(property, value, maxDistance));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria nearSphere(String property, List<?> value) {
        validatePropertyName(property, "nearSphere");
        addToCriteria(new MongoQuery.NearSphere(property, value));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria nearSphere(String property, List<?> value, Number maxDistance) {
        validatePropertyName(property, "nearSphere");
        addToCriteria(new MongoQuery.NearSphere(property, value, maxDistance));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria nearSphere(String property, List<?> value, Distance maxDistance) {
        validatePropertyName(property, "nearSphere");
        addToCriteria(new MongoQuery.NearSphere(property, value, maxDistance));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria nearSphere(String property, Point value) {
        validatePropertyName(property, "nearSphere");
        addToCriteria(new MongoQuery.NearSphere(property, value));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria nearSphere(String property, Point value, Number maxDistance) {
        validatePropertyName(property, "nearSphere");
        addToCriteria(new MongoQuery.NearSphere(property, value, maxDistance));
        return this;
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria nearSphere(String property, Point value, Distance maxDistance) {
        validatePropertyName(property, "nearSphere");
        addToCriteria(new MongoQuery.NearSphere(property, value, maxDistance));
        return this;
    }
    /**
     * Geospacial query for values within a given box. A box is defined as a multi-dimensional list in the form
     *
     * [[40.73083, -73.99756], [40.741404,  -73.988135]]
     *
     * @param property The property
     * @param value A multi-dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria withinBox(String property, List<?> value) {
        validatePropertyName(property, "withinBox");
        addToCriteria(new MongoQuery.WithinBox(property, value));
        return this;
    }

    /**
     * Geospacial query for values within a given polygon. A polygon is defined as a multi-dimensional list in the form
     *
     * [[0, 0], [3, 6], [6, 0]]
     *
     * @param property The property
     * @param value A multi-dimensional list of values
     * @return this Criterion
     */
    public MongoCriteria withinPolygon(String property, List<?> value) {
        validatePropertyName(property, "withinPolygon");
        addToCriteria(new MongoQuery.WithinPolygon(property, value));
        return this;
    }

    /**
     * Geospacial query for values within a given circle. A circle is defined as a multi-dimensial list containing the position of the center and the radius:
     *
     * [[50, 50], 10]
     *
     * @param property The property
     * @param value A multi-dimensional list of values
     * @return The criteria insstance
     */
    public MongoCriteria withinCircle(String property, List<?> value) {
        validatePropertyName(property, "withinBox");
        addToCriteria(new MongoQuery.WithinCircle(property, value));
        return this;
    }

    /**
     * Geospacial query for the given shape returning records that are found within the given shape
     *
     * @param property The property
     * @param shape The shape
     * @return The criteria insstance
     */
    public MongoCriteria geoWithin(String property, Shape shape) {
        validatePropertyName(property, "geoWithin");
        addToCriteria(new MongoQuery.GeoWithin(property, shape));
        return this;
    }

    /**
     * Geospacial query for the given shape returning records that are found to intersect the given shape
     *
     * @param property The property
     * @param shape The shape
     * @return The criteria insstance
     */
    public MongoCriteria geoIntersects(String property, GeoJSON shape) {
        validatePropertyName(property, "geoIntersects");
        addToCriteria(new MongoQuery.GeoIntersects(property, shape));
        return this;
    }

    public MongoCriteria arguments(Map arguments) {
        ((QueryArgumentsAware)this.query).setArguments(arguments);
        return this;
    }
}
