package org.grails.inconsequential.appengine;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import org.grails.inconsequential.core.Connection;
import org.grails.inconsequential.core.DatastoreContext;
import org.grails.inconsequential.mapping.MappingContext;
import org.grails.inconsequential.tx.Transaction;

/**
 * A context of access and execution for Google App Engine's datastore.
 *
 * @author Guillaume Laforge
 * @author Graeme Rocher
 */
public class AppEngineContext extends DatastoreContext {

    /**
     * Create a new context taking an active connection to the store as parameter.
     *
     * @param appEngineConnection a connection to the datastore
     */
    public AppEngineContext(AppEngineConnection appEngineConnection) {
        super(appEngineConnection);
    }

    public AppEngineContext(Connection connection, MappingContext mappingContext) {
        super(connection, mappingContext);
    }

    /**
     * Start a new transaction.
     *
     * @return a started transaction
     */
    @Override
    public Transaction beginTransaction() {
        AppEngineTransaction engineTransaction = new AppEngineTransaction(DatastoreServiceFactory.getDatastoreService().beginTransaction());
        this.transaction = engineTransaction;
        return engineTransaction;
    }
}
