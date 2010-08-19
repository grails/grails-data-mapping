package org.springframework.datastore.jcr.engine;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.datastore.core.Session;
import org.springframework.datastore.jcr.util.JcrConstants;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.node.engine.AbstractNodeEntityPersister;
import org.springframework.datastore.query.Query;
import org.springmodules.jcr.JcrCallback;
import org.springmodules.jcr.JcrTemplate;

import javax.jcr.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.List;


/**
 * TODO: write javadoc
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrEntityPersister extends AbstractNodeEntityPersister<Node, String> {

    private static final Logger log = Logger.getLogger(JcrEntityPersister.class);

    private JcrTemplate jcrTemplate;

    public JcrEntityPersister(MappingContext context, PersistentEntity entity, Session session, JcrTemplate jcrTemplate) {
        super(context, entity, session);
        this.jcrTemplate = jcrTemplate;
        this.jcrTemplate.setAllowCreate(true);
    }

    public Query createQuery() {
        return null;
    }



    @Override
    protected Node retrieveEntry(PersistentEntity persistentEntity, final Serializable key) {
        return (Node) jcrTemplate.execute(new JcrCallback() {
            public Object doInJcr(javax.jcr.Session session) throws IOException, RepositoryException {
                return session.getNodeByUUID(getString(key));
            }
        });

    }

    @Override
    protected String storeEntry(PersistentEntity persistentEntity, Node nativeEntry) {
        final PersistentEntity entity = persistentEntity;
        final Node node = nativeEntry;
        return (String) jcrTemplate.execute(new JcrCallback() {
            public Object doInJcr(javax.jcr.Session session) throws IOException, RepositoryException {
                log.info("Executing node: " + node.getPath());
                session.save();
                return node.getUUID();                 
            }
        });
    }

    @Override
    protected Node createNewEntry(final String name) {
        log.info("Create new entry name: " + name);
        return (Node) jcrTemplate.execute(new JcrCallback() {
            public Object doInJcr(javax.jcr.Session session) throws IOException, RepositoryException {
                Node rootNode = session.getRootNode();
                Node node = rootNode.addNode(name, JcrConstants.DEFAULT_JCR_TYPE);
                node.addMixin(JcrConstants.MIXIN_REFERENCEABLE);
                return node;
            }
            ;
        });
    }

    

    @Override
    protected void updateEntry(PersistentEntity persistentEntity, String id, Node entry) {
        //TODO. Implement updateEntry method 
    }

    @Override
    protected Object getEntryValue(Node nativeEntry, String property) {
        try {
            Property prop = nativeEntry.getProperty(property);
            switch(prop.getType()){
                case PropertyType.STRING:
                    return prop.getString();
                case PropertyType.BINARY:
                    return prop.getString();
                case PropertyType.DATE:
                    return prop.getString();
                case PropertyType.DOUBLE:
                    return prop.getString();
                case PropertyType.LONG:
                    return prop.getString();
                case PropertyType.BOOLEAN:
                    return prop.getString();
                case PropertyType.NAME:
                    return prop.getString();
                case PropertyType.PATH:
                    return prop.getString();
                case PropertyType.REFERENCE:
                    return prop.getString();
                default:return null;
            }
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Exception occurred cannot getProperty from Node: " + e.getMessage(), e);
        }
    }

    @Override
    protected void setEntryValue(Node nativeEntry, String propertyName, Object value) {
        if (value != null) {
            log.info("propertyName : " + propertyName + " value object: " + value.toString());
            try {
                if (value instanceof Boolean) {
                    nativeEntry.setProperty(propertyName, (Boolean) value);
                } else if (value instanceof Calendar) {
                    nativeEntry.setProperty(propertyName, (Calendar) value);
                } else if (value instanceof Double) {
                    nativeEntry.setProperty(propertyName, (Double) value);
                } else if (value instanceof InputStream) {
                    nativeEntry.setProperty(propertyName, (InputStream) value);
                } else if (value instanceof Long) {
                    nativeEntry.setProperty(propertyName, (Long) value);
                } else if (value instanceof Node) {
                    nativeEntry.setProperty(propertyName, (Node) value);
                } else if (value instanceof String) {
                    nativeEntry.setProperty(propertyName, (String) value);
                }
            } catch (RepositoryException e) {
                throw new DataAccessResourceFailureException("Exception occurred set a property value to Node: " + e.getMessage(), e);
            }
        }
    }


    private String getString(Object key) {
        return typeConverter.convertIfNecessary(key, String.class);
    }

   
}


