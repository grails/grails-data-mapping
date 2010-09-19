package org.springframework.datastore.mapping.jcr;

import org.apache.jackrabbit.core.TransientRepository;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.datastore.mapping.core.AbstractSession;
import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.engine.Persister;
import org.springframework.datastore.mapping.jcr.engine.JcrEntityPersister;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.transactions.Transaction;
import org.springframework.extensions.jcr.JcrSessionFactory;
import org.springframework.extensions.jcr.JcrTemplate;
import org.springframework.extensions.jcr.support.OpenSessionInViewInterceptor;
import org.springframework.transaction.TransactionSystemException;


import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.util.Map;

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrSession extends AbstractSession<JcrSessionFactory> {

    //private Repository repository;
    private JcrSessionFactory jcrSessionFactory;
    private OpenSessionInViewInterceptor interceptor;

    public JcrSession(Datastore ds,MappingContext mappingContext,JcrSessionFactory jcrSessionFactory) {
        super(ds, mappingContext);
        try {
            //repository = new (Repository)TransientRepository();
            //jcrSessionFactory = new JcrSessionFactory(repository, connectionDetails.get("workspace"),
            //                                                      new SimpleCredentials(connectionDetails.get("username"), connectionDetails.get("password").toCharArray()));

            //jcrSessionFactory.afterPropertiesSet();*/
            this.jcrSessionFactory = jcrSessionFactory;
            interceptor = new OpenSessionInViewInterceptor();
            interceptor.setSessionFactory(this.jcrSessionFactory);
            interceptor.preHandle(null, null, null);
        } catch (Exception e) {
            throw new DataAccessResourceFailureException("Cannot connect to repository: " + e.getMessage(), e);
        }
    }

   @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        if(entity != null) {
          return new JcrEntityPersister(mappingContext, entity, this, new JcrTemplate(jcrSessionFactory));
        }
        return null;
    }

    @Override
    public void disconnect(){
        try{
            //interceptor.afterCompletion(null, null, null, null);
            ((TransientRepository)jcrSessionFactory.getRepository()).shutdown();
        }catch (Exception e) {
           throw new DataAccessResourceFailureException("Failed to disconnect JCR Repository: " + e.getMessage(), e);
        }finally {
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

    
    @Override
    protected Transaction beginTransactionInternal() {
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
