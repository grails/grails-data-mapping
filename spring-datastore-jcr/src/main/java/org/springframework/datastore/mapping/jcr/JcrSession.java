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
import org.springframework.extensions.jcr.*;
import org.springframework.extensions.jcr.support.OpenSessionInViewInterceptor;


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

    protected OpenSessionInViewInterceptor interceptor = new OpenSessionInViewInterceptor();  

    private JcrSessionFactory jcrSessionFactory;
    public JcrSession(Datastore ds,MappingContext mappingContext,JcrSessionFactory jcrSessionFactory) {
        super(ds, mappingContext);
        this.jcrSessionFactory = jcrSessionFactory;
         interceptor.setSessionFactory(jcrSessionFactory);
         interceptor.preHandle(null, null, null);
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
        interceptor.afterCompletion(null, null, null, null);    
        try{
            ((TransientRepository)jcrSessionFactory.getRepository()).shutdown();
            super.disconnect();
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
        return new JcrTransaction(jcrSessionFactory);           
    }

    public Session getNativeInterface() {
        try {
            return jcrSessionFactory.getSession();
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Session not found: " + e.getMessage(), e);
        }
    }
}

