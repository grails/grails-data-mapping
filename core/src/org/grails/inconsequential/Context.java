package org.grails.inconsequential;

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
 */
public abstract class Context {
    private Connection connection;
    private Session session;
    private Transaction transaction;

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public abstract Transaction beginTransaction();
}
