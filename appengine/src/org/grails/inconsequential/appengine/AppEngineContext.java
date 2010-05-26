package org.grails.inconsequential.appengine;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import org.grails.inconsequential.core.Context;
import org.grails.inconsequential.tx.Transaction;

/**
 * A context of access and execution for Google App Engine's datastore.
 *
 * @author Guillaume Laforge
 */
public class AppEngineContext extends Context {

    /**
     * Create a new context taking an active connection to the store as parameter.
     *
     * @param appEngineConnection a connection to the datastore
     */
    public AppEngineContext(AppEngineConnection appEngineConnection) {
        super();
        setConnection(appEngineConnection);
    }

    /**
     * Start a new transaction.
     *
     * @return a started transaction
     */
    @Override
    public Transaction beginTransaction() {
        AppEngineTransaction engineTransaction = new AppEngineTransaction(DatastoreServiceFactory.getDatastoreService().beginTransaction());
        setTransaction(engineTransaction);
        return engineTransaction;
    }
}
