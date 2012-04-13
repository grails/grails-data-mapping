package org.grails.datastore.mapping.dynamodb.util;

public class DataStoreOperationException extends org.springframework.dao.DataAccessException {
    public DataStoreOperationException(String msg) {
        super(msg);
    }

    public DataStoreOperationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}