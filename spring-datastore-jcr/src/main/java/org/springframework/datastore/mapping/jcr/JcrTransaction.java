package org.springframework.datastore.mapping.jcr;

import org.springframework.datastore.mapping.transactions.Transaction;
import org.springframework.extensions.jcr.JcrTemplate;


/**
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrTransaction implements Transaction<JcrTemplate> {
    //TODO: Implement Transaction support for JCR

    public void commit() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void rollback() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public JcrTemplate getNativeTransaction() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isActive() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setTimeout(int timeout) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
