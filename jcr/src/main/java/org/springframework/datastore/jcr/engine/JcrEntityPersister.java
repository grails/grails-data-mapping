package org.springframework.datastore.jcr.engine;

import org.apache.derby.iapi.services.classfile.ClassMember;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.datastore.core.Session;
import org.springframework.datastore.engine.AssociationIndexer;
import org.springframework.datastore.engine.PropertyValueIndexer;
import org.springframework.datastore.jcr.uuid.UUIDUtil;
import org.springframework.datastore.keyvalue.engine.AbstractKeyValueEntityPesister;
import org.springframework.datastore.mapping.ClassMapping;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.mapping.PersistentProperty;
import org.springframework.datastore.mapping.types.Association;
import org.springframework.datastore.node.AbstractNodeEnityPersister;
import org.springframework.datastore.query.Query;
import org.springmodules.jcr.JcrCallback;
import org.springmodules.jcr.JcrTemplate;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;


/**
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrEntityPersister extends AbstractNodeEnityPersister {//extends AbstractKeyValueEntityPesister<Node, String> {
    private JcrTemplate jcrTemplate;

    public JcrEntityPersister(MappingContext context, PersistentEntity entity, Session session) {
        super(context, entity, session);
    }

    public JcrEntityPersister(MappingContext context, PersistentEntity entity,Session session, JcrTemplate jcrTemplate){
        super(context, entity, session);
        this.jcrTemplate = jcrTemplate;
        this.jcrTemplate.setAllowCreate(true);
    }

   /* @Override
    protected String storeEntry(final PersistentEntity persistentEntity, Node nativeEntry) {
       final UUID uuid = UUIDUtil.getTimeUUID();
       return (String)jcrTemplate.execute(new JcrCallback() {
            public Object doInJcr(javax.jcr.Session session) throws IOException, RepositoryException {
                Node rootNode = session.getRootNode();
                Node entity = rootNode.addNode(persistentEntity.getName());
                entity.addMixin("mix:referenceable");
                entity.addMixin("mix:versionable");
                entity.addMixin("mix:lockable");
                for(PersistentProperty property : persistentEntity.getPersistentProperties()){
                    try{
                        if(property.getName().equals("id")){
                            entity.setProperty("id",uuid.toString());
                        }else{
                            entity.setProperty(property.getName(), persistentEntity.getJavaClass().getDeclaredField(property.getName()).toString());
                            System.out.println(persistentEntity.getJavaClass().getDeclaredField(property.getName()).toString());
                        }
                    }catch(NoSuchFieldException e){
                        throw new DataAccessResourceFailureException("Exception occurred mapping Fields to Node: " + e.getMessage(),e);
                    }
                }
                session.save();
                return entity.getUUID();
            }
        });
    }*/
    

    public Query createQuery() {
        return null;  
    }
}
