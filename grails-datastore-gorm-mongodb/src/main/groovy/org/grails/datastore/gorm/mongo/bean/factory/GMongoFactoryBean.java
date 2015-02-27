/* Copyright (C) 2010 SpringSource
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.grails.datastore.gorm.mongo.bean.factory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.gmongo.GMongoClient;
import com.mongodb.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grails.datastore.mapping.model.DatastoreConfigurationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import com.gmongo.GMongo;

/**
 * A Factory bean for initializing a {@link GMongo} instance
 *
 * @author Graeme Rocher
 */
public class GMongoFactoryBean implements FactoryBean<GMongoClient>, InitializingBean/*,
    PersistenceExceptionTranslator*/, DisposableBean {

    /**
     * Logger, available to subclasses.
     */
    protected final Log logger = LogFactory.getLog(getClass());

    private MongoClient mongo;
    private MongoClientOptions mongoOptions;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String database;
    private List<ServerAddress> replicaSetSeeds;
    private List<ServerAddress> replicaPair;
    private String connectionString;
    private MongoClientURI clientURI;

    public void setReplicaPair(List<ServerAddress> replicaPair) {
        this.replicaPair = replicaPair;
    }

    public void setReplicaSetSeeds(List<ServerAddress> replicaSetSeeds) {
        this.replicaSetSeeds = replicaSetSeeds;
    }

    public void setMongoOptions(MongoClientOptions mongoOptions) {
        this.mongoOptions = mongoOptions;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDatabase(String databaseName) {
        this.database = databaseName;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public void setClientURI(MongoClientURI clientURI) {
        this.clientURI = clientURI;
    }

    public GMongoClient getObject() throws Exception {
        Assert.notNull(mongo, "Mongo must not be null");
        GMongoClient mongoClient = new GMongoClient();
        mongoClient.setMongoClient(mongo);
        return mongoClient;
    }

    public Class<? extends GMongo> getObjectType() {
        return GMongo.class;
    }

    public boolean isSingleton() {
        return false;
    }

    public void afterPropertiesSet() throws UnknownHostException {
        // apply defaults - convenient when used to configure for tests
        // in an application context
        if (mongo != null) {
            return;
        }

        ServerAddress defaultOptions = new ServerAddress();
        List<MongoCredential> credentials = new ArrayList<MongoCredential>();
        if (mongoOptions == null) {
            MongoClientOptions.Builder builder = MongoClientOptions.builder();


            mongoOptions = builder.build();
        }
        // If username/pw exists and we are not authenticated, authenticate now
        if (username != null && password != null) {
            credentials.add(MongoCredential.createCredential(username, database, password.toCharArray()));
        }

        if (replicaPair != null) {
            if (replicaPair.size() < 2) {
                throw new DatastoreConfigurationException("A replica pair must have two server entries");
            }
            mongo = new MongoClient(replicaPair, credentials, mongoOptions);
        }
        else if (replicaSetSeeds != null) {
            mongo = new MongoClient(replicaSetSeeds, credentials, mongoOptions);
        }
        else if(clientURI != null) {
            mongo = new MongoClient(clientURI);
        }
        else if(connectionString != null) {
            mongo = new MongoClient(new MongoClientURI(connectionString));
        }
        else {
            String mongoHost = host != null ? host : defaultOptions.getHost();
            if (port != null) {
                mongo = new MongoClient(new ServerAddress(mongoHost, port), credentials, mongoOptions);
            }
            else {
                mongo = new MongoClient(new ServerAddress(host), credentials,  mongoOptions);
            }
        }

    }

    public void destroy() {
        if(mongo == null) {
            return;
        }

        mongo.close();
        mongo = null;
    }
}
