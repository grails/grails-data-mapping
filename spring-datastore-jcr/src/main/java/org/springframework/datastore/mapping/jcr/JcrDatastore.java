package org.springframework.datastore.mapping.jcr;

import org.apache.jackrabbit.core.TransientRepository;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.node.mapping.NodeMappingContext;
import org.springframework.extensions.jcr.JcrSessionFactory;

import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;
import java.util.Map;

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrDatastore  extends AbstractDatastore {

    public JcrDatastore(MappingContext mappingContext) {
        super(mappingContext);
    }
    public JcrDatastore(){
        super(new NodeMappingContext());
    }

    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        System.setProperty("org.apache.jackrabbit.repository.conf", connectionDetails.get("configuration"));
        System.setProperty("org.apache.jackrabbit.repository.home", connectionDetails.get("homeDir"));
        Repository repository = null;
        JcrSessionFactory jcrSessionFactory = null;
        try {
            repository = new TransientRepository();
            jcrSessionFactory = new JcrSessionFactory(repository, connectionDetails.get("workspace"),
                    new SimpleCredentials(connectionDetails.get("username"), connectionDetails.get("password").toCharArray()));


            jcrSessionFactory.afterPropertiesSet();
        } catch (Exception e) {
            throw new DataAccessResourceFailureException("Exception occurred cannot getProperty from Node: " + e.getMessage(), e);
        }
        return new JcrSession(this, getMappingContext(),jcrSessionFactory);
    }

}
