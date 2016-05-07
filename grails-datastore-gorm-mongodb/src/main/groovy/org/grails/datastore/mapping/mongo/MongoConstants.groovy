package org.grails.datastore.mapping.mongo

import groovy.transform.CompileStatic

/**
 * Constants for use with GORM for MongoDB
 *
 * @since 6.0
 * @author Graeme Rocher
 */
@CompileStatic
class MongoConstants {
    public static final String SET_OPERATOR = '$set';
    public static final String UNSET_OPERATOR = '$unset';

    public static final String MONGO_ID_FIELD = "_id";
    public static final String MONGO_CLASS_FIELD = "_class";
    public static final String INC_OPERATOR = '$inc'
    public static final String ASSIGNED_IDENTIFIER_MAPPING = "assigned"

}
