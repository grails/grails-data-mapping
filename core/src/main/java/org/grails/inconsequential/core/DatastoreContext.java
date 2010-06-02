package org.grails.inconsequential.core;

import org.grails.inconsequential.mapping.MappingContext;
import org.grails.inconsequential.tx.Transaction;

/**
 * The context can hold information about
 * <ul>
 *  <li>Connection: the connection to access the datastore</li>
 *  <li>Session: the session used to interact with the datastore</li>
 *  <li>Transaction: the transaction within which to execute operations on the datastore</li>
 * </ul>
 * The class is abstract so that all implementations of the protocol
 * provides its own concrete implementation of the context class.
 *
 * @author Guillaume Laforge
 * @author Graeme Rocher
 *
 * @since 1.0
 */
public abstract class DatastoreContext {
    protected Connection connection;
    protected Session session;
    protected Transaction transaction;
    protected MappingContext mappingContext;

    public DatastoreContext(Connection connection, MappingContext mappingContext) {
        this.connection = connection;
        this.mappingContext = mappingContext;
    }

    protected DatastoreContext(Connection connection) {
        this.connection = connection;
    }

    /**
     * @return The Datastore connection
     */
    public Connection getConnection() {
        return connection;
    }

    public Session getSession() {
        return session;
    }
    /**
     * @return The current Datastore transaction
     */
    public Transaction currentTransaction() {
        return transaction;
    }

    public abstract Transaction beginTransaction();

    public MappingContext getMappingContext() {
        return this.mappingContext;
    }
}
