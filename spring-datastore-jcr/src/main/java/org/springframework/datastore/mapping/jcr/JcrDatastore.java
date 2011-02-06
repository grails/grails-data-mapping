package org.springframework.datastore.mapping.jcr;

import org.apache.jackrabbit.core.TransientRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.node.mapping.NodeMappingContext;
import org.springframework.extensions.jcr.JcrSessionFactory;

import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;
import java.util.Date;
import java.util.Map;

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrDatastore  extends AbstractDatastore {

    private static String REPOSITORY_CONF = "classpath:repository.xml";
    private static String REPOSITORY_HOME = "/tmp/repo";  //TODO: must change the directory to root classpath
    private static String DEFAULT_WORKSPACE = "default";
    private static String DEFAULT_USERNAME = "username";
    private static String DEFAULT_PASSWORD = "password";

    public JcrDatastore(MappingContext mappingContext) {
        super(mappingContext);
    }
    public JcrDatastore(){
        super(new NodeMappingContext());
    }


    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        String workspace = null;
        String username = null;
        String password = null;
        if(connectionDetails != null){
           if(connectionDetails.get("configuration") != null)
                System.setProperty("org.apache.jackrabbit.repository.conf", connectionDetails.get("configuration"));
           else
               System.setProperty("org.apache.jackrabbit.repository.conf", REPOSITORY_CONF);
           if(connectionDetails.get("homeDir") != null)
               System.setProperty("org.apache.jackrabbit.repository.home", connectionDetails.get("homeDir"));
           else
               System.setProperty("org.apache.jackrabbit.repository.home", REPOSITORY_HOME);
            workspace = connectionDetails.get("workspace");
            username = connectionDetails.get("username");
            password = connectionDetails.get("password");            
        }else{
            System.setProperty("org.apache.jackrabbit.repository.conf", REPOSITORY_CONF);
            System.setProperty("org.apache.jackrabbit.repository.home", REPOSITORY_HOME);
            workspace = DEFAULT_WORKSPACE;
            username = DEFAULT_USERNAME;
            password = DEFAULT_PASSWORD;
        }
        Repository repository = null;
        JcrSessionFactory jcrSessionFactory = null;
        try {
            repository = new TransientRepository();
            jcrSessionFactory = new JcrSessionFactory(repository, workspace, new SimpleCredentials(username, password.toCharArray()));
            jcrSessionFactory.afterPropertiesSet();
        } catch (Exception e) {
            throw new DataAccessResourceFailureException("Exception occurred cannot create Session: " + e.getMessage(), e);
        }
        return new JcrSession(this, getMappingContext(),jcrSessionFactory);
    }






}
