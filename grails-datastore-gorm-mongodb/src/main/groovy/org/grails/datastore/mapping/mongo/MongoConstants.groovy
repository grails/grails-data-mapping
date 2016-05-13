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

    public static final String SETTING_DATABASE_NAME = "grails.mongodb.databaseName";
    public static final String SETTING_CONNECTION_STRING = "grails.mongodb.connectionString";
    public static final String SETTING_URL = "grails.mongodb.url";
    public static final String SETTING_DEFAULT_MAPPING = "grails.mongodb.default.mapping";
    public static final String SETTING_OPTIONS = "grails.mongodb.options";
    public static final String SETTING_HOST = "grails.mongodb.host";
    public static final String SETTING_PORT = "grails.mongodb.port";
    public static final String SETTING_USERNAME = "grails.mongodb.username";
    public static final String SETTING_PASSWORD = "grails.mongodb.password";
}
