package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.gorm.neo4j.Neo4jSession;
import org.grails.datastore.mapping.core.DatastoreUtils;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager;
import org.grails.datastore.mapping.transactions.Transaction;
import org.grails.datastore.mapping.transactions.TransactionObject;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.FlushModeType;

/**
 * @author Stefan Armbruster
 */

public class Neo4jDatastoreTransactionManager extends DatastoreTransactionManager {

    @Override
        protected void doBegin(Object o, TransactionDefinition definition) throws TransactionException {
            TransactionObject txObject = (TransactionObject) o;

            Neo4jSession session = null;
            try {
                session = (Neo4jSession) txObject.getSessionHolder().getSession();

                if (definition.isReadOnly()) {
                    // Just set to NEVER in case of a new Session for this transaction.
                    session.setFlushMode(FlushModeType.COMMIT);
                }

                Transaction<?> tx = session.beginTransaction(definition);
                // Register transaction timeout.
                int timeout = determineTimeout(definition);
                if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
                    tx.setTimeout(timeout);
                }

                // Add the Datastore transaction to the session holder.
                txObject.setTransaction(tx);

                // Bind the session holder to the thread.
                if (txObject.isNewSessionHolder()) {
                    TransactionSynchronizationManager.bindResource(getDatastore(), txObject.getSessionHolder());
                }
                txObject.getSessionHolder().setSynchronizedWithTransaction(true);
            }
            catch (Exception ex) {
                if (txObject.isNewSession()) {
                    try {
                        if (session != null && session.getTransaction().isActive()) {
                            session.getTransaction().rollback();
                        }
                    }
                    catch (Throwable ex2) {
                        logger.debug("Could not rollback Session after failed transaction begin", ex);
                    }
                    finally {
                        DatastoreUtils.closeSession(session);
                    }
                }
                throw new CannotCreateTransactionException("Could not open Datastore Session for transaction", ex);
            }
        }}
