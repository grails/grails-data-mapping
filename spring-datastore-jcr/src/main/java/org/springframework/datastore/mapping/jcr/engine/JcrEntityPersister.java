package org.springframework.datastore.mapping.jcr.engine;


import org.springframework.beans.SimpleTypeConverter;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.jcr.JcrSession;
import org.springframework.datastore.mapping.jcr.util.JcrConstants;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.node.engine.AbstractNodeEntityPersister;
import org.springframework.datastore.mapping.query.JcrQuery;
import org.springframework.extensions.jcr.JcrCallback;
import org.springframework.extensions.jcr.JcrTemplate;

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

    private static String parentNodeUUID = null;
    private static String rootParentUUID = null;
    private static Class clazz = null;

    private JcrTemplate jcrTemplate;
    private SimpleTypeConverter typeConverter;
    public static final boolean DISCRIMINATOR = true;


    public JcrEntityPersister(MappingContext context, PersistentEntity entity, Session session, JcrTemplate jcrTemplate) {
        super(context, entity, session);
        typeConverter = new SimpleTypeConverter();
        this.jcrTemplate = jcrTemplate;
        //this.jcrTemplate.setAllowCreate(true);
    }

    public JcrEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session) {
        super(mappingContext, entity, session);
    }

  
   @Override
    protected String generateIdentifier(PersistentEntity persistentEntity, Node tmp) {
        try {
            return tmp.getUUID();
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Exception occurred cannot getUUID from Node: " + e.getMessage(), e);
        }

    }

    @Override
    protected void deleteEntry(final String key) {
        System.out.println("deleteEntry");

        Node node = jcrTemplate.getNodeByUUID(key);
        try {
            node.remove();
            jcrTemplate.save();
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Exception occurred cannot delete Node: " + e.getMessage(), e);
        }
    }

     @Override
    protected Object getEntryValue(Node nativeEntry, String property) {
        try {
           Property prop = nativeEntry.getProperty(property);
            if (prop.getType() == PropertyType.REFERENCE) {
                String nodeUuid = prop.getString();
                return jcrTemplate.getNodeByUUID(nodeUuid);
            } else {
                switch (prop.getType()) {
                    case PropertyType.BINARY:
                        // TODO - add lazyinputstream
                        return prop.getString();
                    case PropertyType.BOOLEAN:
                        return prop.getBoolean();
                    case PropertyType.DATE:
                        return prop.getDate();
                    case PropertyType.DOUBLE:
                        return prop.getDouble();
                    case PropertyType.LONG:
                        return prop.getLong();
                    case PropertyType.NAME: // fall through
                    case PropertyType.PATH: // fall through
                    case PropertyType.REFERENCE: // fall through
                    case PropertyType.STRING: // fall through
                    case PropertyType.UNDEFINED: // not actually expected
                    default: // not actually expected
                        return prop.getString();
                }
            }

        } catch (Exception e) {
            throw new DataAccessResourceFailureException("Exception occurred cannot getProperty from Node: " + e.getMessage(), e);
        }
    }

    //TODO Re-implement retrieveEntry to use Query instead.

    @Override
    protected Node retrieveEntry(final PersistentEntity persistentEntity, final Serializable key) {
        System.out.println("retrieveEntry");
        return (Node) jcrTemplate.execute(new JcrCallback() {
            public Object doInJcr(javax.jcr.Session session) throws IOException, RepositoryException {
               // Workspace ws = session.getWorkspace();
                //QueryManager qm = ws.getQueryManager();
               // Query q = qm.createQuery("//"+persistentEntity.getJavaClass().getSimpleName()+"[@id = '"+getString(key)+"']",Query.XPATH);
               // System.out.println(q.getStatement());
                try {
                    return session.getNodeByUUID(getString(key));
                } catch (ItemNotFoundException ex) {   //getNodeByUUID always throws ItemNotFoundException when Node doesn't exist
                    return null;
                }
            }
        });
    }

      private String getString(Object key) {
        return typeConverter.convertIfNecessary(key, String.class);
    }


    //TODO Write Document how createNewEntry Method Work
    @Override
    protected  Node createNewEntry(final PersistentEntity persistentEntity) {
        try {
            Node node = null;
            if (parentNodeUUID == null && rootParentUUID == null) {
                Node rootNode = jcrTemplate.getRootNode();
                node = rootNode.addNode(persistentEntity.getJavaClass().getSimpleName(), JcrConstants.DEFAULT_JCR_TYPE);
                node.addMixin(JcrConstants.MIXIN_REFERENCEABLE);
                rootParentUUID = node.getUUID();
                parentNodeUUID = node.getUUID();
            } else if (rootParentUUID != null && parentNodeUUID != null && (clazz != null && clazz.equals(persistentEntity.getJavaClass()))) {
                Node parentNode = jcrTemplate.getNodeByUUID(rootParentUUID);
                node = parentNode.addNode(persistentEntity.getJavaClass().getSimpleName(), JcrConstants.DEFAULT_JCR_TYPE);
                node.addMixin(JcrConstants.MIXIN_REFERENCEABLE);
                parentNodeUUID = node.getUUID();
            } else {
                Node parentNode = jcrTemplate.getNodeByUUID(parentNodeUUID);
                node = parentNode.addNode(persistentEntity.getJavaClass().getSimpleName(), JcrConstants.DEFAULT_JCR_TYPE);
                node.addMixin(JcrConstants.MIXIN_REFERENCEABLE);
                clazz = persistentEntity.getJavaClass();
                parentNodeUUID = node.getUUID();
            }
            node.addMixin(JcrConstants.MIXIN_VERSIONABLE);
            node.addMixin(JcrConstants.MIXIN_LOCKABLE);
            return node;
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Exception occurred cannot create Node: " + e.getMessage(), e);
        }
    }


    @Override
    protected void setEntryValue(Node nativeEntry, String propertyName, Object value) {
        if (value != null) {
            try {
                if (value instanceof String) {
                    nativeEntry.setProperty(propertyName, (String) value);
                } else if (value instanceof Boolean) {
                    nativeEntry.setProperty(propertyName, (Boolean) value);
                } else if (value instanceof Calendar) {
                    nativeEntry.setProperty(propertyName, (Calendar) value);
                } else if (value instanceof Double) {
                    nativeEntry.setProperty(propertyName, (Double) value);
                } else if (value instanceof InputStream) {
                    nativeEntry.setProperty(propertyName, (InputStream) value);
                } else if (value instanceof Long) {
                    nativeEntry.setProperty(propertyName, (Long) value);
                }
            } catch (RepositoryException e) {
                throw new DataAccessResourceFailureException("Exception occurred set a property value to Node: " + e.getMessage(), e);
            }
        }
    }

    @Override
    protected String storeEntry(PersistentEntity persistentEntity, String id, Node nativeEntry) {
        jcrTemplate.save();
        return id;
    }

    @Override
    protected void updateEntry(final PersistentEntity persistentEntity, final String id, final Node entry) {
        if(id != null) {
        jcrTemplate.execute(new JcrCallback() {
            public Object doInJcr(javax.jcr.Session session) throws IOException, RepositoryException {
                List<String> pns = persistentEntity.getPersistentPropertyNames();                
                Node node = session.getNodeByUUID(id);
                node.checkout();
                for (String propName : pns) {
                    node.setProperty(propName, entry.getProperty(propName).getValue());
                }
                node.save();
                node.checkin();
                return null;
            }
        });
        }
    }


    public org.springframework.datastore.mapping.query.Query createQuery() {
        System.out.println("createQuery");
        return new JcrQuery((JcrSession) getSession(), getJcrTemplate(), getPersistentEntity(), this);  //To change body of implemented methods use File | Settings | File Templates.
    }

    public JcrTemplate getJcrTemplate() {
        return jcrTemplate;
    }
}

