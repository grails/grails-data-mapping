package org.grails.datastore.mapping.dynamodb.util;

import org.springframework.dao.DataAccessException;

public class DataStoreOperationException extends DataAccessException {
    private static final long serialVersionUID = 1;

    public DataStoreOperationException(String msg) {
        super(msg);
    }

    public DataStoreOperationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}