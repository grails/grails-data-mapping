package org.springframework.datastore.mapping.jcr.engine;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.engine.AssociationIndexer;
import org.springframework.datastore.mapping.jcr.util.JcrConstants;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.types.Association;
import org.springframework.datastore.mapping.node.engine.AbstractNodeEntityPersister;
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

    private JcrTemplate jcrTemplate;
    private SimpleTypeConverter typeConverter;

    public JcrEntityPersister(MappingContext context, PersistentEntity entity, Session session, JcrTemplate jcrTemplate) {
        super(context, entity, session);
        typeConverter = new SimpleTypeConverter();
        this.jcrTemplate = jcrTemplate;
        this.jcrTemplate.setAllowCreate(true);
    }

    public JcrEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session) {
        super(mappingContext, entity, session);
    }

    @Override
    protected void setEntryAssociatedValue(Node parentEntity, String property, Serializable associationId) {
       Node node = jcrTemplate.getNodeByUUID((String) associationId);
        try {
            parentEntity.setProperty(property,node);
            Property pro = parentEntity.getProperty(property);
            System.out.println(pro.getNode().getPath());
            System.out.println(pro.getNode().getParent().getPath());
            //System.out.println("path: "+ node.getPath());
            //System.out.println("moved path: "+parentEntity.getPath() + node.getPath());
            //jcrTemplate.move(node.getPath(),parentEntity.getPath() + node.getPath());
        } catch (RepositoryException e) {
            e.printStackTrace();  
        }
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
        Node node = jcrTemplate.getNodeByUUID(key);
        try {
            node.remove();
            jcrTemplate.save();
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Exception occurred cannot delete Node: " + e.getMessage(), e);
        }
    }

    @Override
    public AssociationIndexer getAssociationIndexer(Association association) {
        return null;
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

    @Override
    protected Node retrieveEntry(PersistentEntity persistentEntity, Serializable key) {
        return jcrTemplate.getNodeByUUID((String) key);
    }

    @Override
    protected Node createNewEntry(final PersistentEntity persistentEntity) {
        return (Node) jcrTemplate.execute(new JcrCallback() {
            public Object doInJcr(javax.jcr.Session session) throws IOException, RepositoryException {
                Node rootNode = session.getRootNode();
                Node node = rootNode.addNode(persistentEntity.getJavaClass().getSimpleName(), JcrConstants.DEFAULT_JCR_TYPE);
                node.addMixin(JcrConstants.MIXIN_REFERENCEABLE);
                node.addMixin(JcrConstants.MIXIN_VERSIONABLE);
                node.addMixin(JcrConstants.MIXIN_LOCKABLE);
                return node;
            }
        });
    }

    @Override
    protected void setEntryValue(Node nativeEntry, String propertyName, Object value) {
        if (value != null) {
            try {                  
                if (value instanceof String){
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

