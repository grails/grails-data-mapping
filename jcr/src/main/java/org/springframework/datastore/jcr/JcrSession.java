package org.springframework.datastore.jcr;

import org.apache.jackrabbit.core.TransientRepository;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.datastore.core.AbstractSession;
import org.springframework.datastore.engine.Persister;
import org.springframework.datastore.jcr.engine.JcrEntityPersister;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.tx.Transaction;
import org.springframework.transaction.TransactionSystemException;
import org.springmodules.jcr.JcrSessionFactory;
import org.springmodules.jcr.JcrTemplate;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.io.IOException;
import java.util.Map;

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrSession extends AbstractSession<Repository> {
    private Repository repository;
    private JcrSessionFactory jcrSessionFactory;

    public JcrSession(Map<String, String> connectionDetails, MappingContext mappingContext) {
        super(connectionDetails, mappingContext);
        try {
            repository = new TransientRepository();
            jcrSessionFactory = new JcrSessionFactory(repository, connectionDetails.get("workspace"),
                                                                  new SimpleCredentials(connectionDetails.get("username"), connectionDetails.get("password").toCharArray()));

            jcrSessionFactory.afterPropertiesSet();
        } catch (Exception e) {
            throw new DataAccessResourceFailureException("Cannot connect to repository: " + e.getMessage(), e);              
        }
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        if(entity != null) {
          return new JcrEntityPersister(mappingContext, entity,this, jcrSessionFactory);
        }
        return null;
    }

    @Override //TODO: Fix the method - Session still alive after calling logout()
    public void disconnect(){
         try {
             jcrSessionFactory.getSession().logout();
        } catch (Exception e) {
            throw new DataAccessResourceFailureException("Failed to destroy JCR session: " + e.getMessage(), e);
        } finally {
            super.disconnect();
        }
    }

    public boolean isConnected() {
        try {
            return jcrSessionFactory.getSession().isLive();
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Repository Errors: " + e.getMessage(), e);
        }
    }

    public Transaction beginTransaction() {
           throw new TransactionSystemException("Transactions have not yet implemented");
    }

    public Session getNativeInterface() {
        try {
            return jcrSessionFactory.getSession();
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Session not found: " + e.getMessage(), e);
        }
    }
    
}
