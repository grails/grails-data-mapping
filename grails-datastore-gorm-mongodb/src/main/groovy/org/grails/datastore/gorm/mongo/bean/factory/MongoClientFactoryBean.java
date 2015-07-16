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
import java.util.*;

import com.mongodb.*;
import groovy.lang.GString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.codehaus.groovy.runtime.GStringImpl;
import org.grails.datastore.mapping.model.DatastoreConfigurationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;


/**
 * A Factory bean for initializing a {@link com.mongodb.MongoClient} instance
 *
 * @author Graeme Rocher
 */
public class MongoClientFactoryBean implements FactoryBean<MongoClient>, InitializingBean/*,
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
    private List<CodecRegistry> codecRegistries = new ArrayList<CodecRegistry>() { {
        add(MongoClient.getDefaultCodecRegistry());
        add(new DefaultGrailsCodecRegistry());

    }};

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

    @Autowired(required = false)
    public void setCodecRegistries(Collection<CodecRegistry> codecRegistries) {
        this.codecRegistries.addAll(codecRegistries);
    }

    public MongoClient getObject() throws Exception {
        return mongo;
    }

    public Class<? extends MongoClient> getObjectType() {
        return MongoClient.class;
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
            builder.codecRegistry(
                    CodecRegistries.fromRegistries( codecRegistries )
            );
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

    public static class DefaultGrailsCodecRegistry implements CodecRegistry {

        private static Map<Class, Codec> CODECS  = new HashMap<Class, Codec>() {
            {
                put(GStringImpl.class, new Codec() {
                    @Override
                    public Object decode(BsonReader reader, DecoderContext decoderContext) {
                        return reader.readString();
                    }

                    @Override
                    public void encode(BsonWriter writer, Object value, EncoderContext encoderContext) {
                        writer.writeString(value.toString());
                    }

                    @Override
                    public Class getEncoderClass() {
                        return GString.class;
                    }
                });
            }
        };

        @Override
        public <T> Codec<T> get(Class<T> clazz) {
            return CODECS.get(clazz);
        }
    }
}
