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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.grails.datastore.mapping.model.DatastoreConfigurationException;
import org.springframework.util.Assert;

import com.gmongo.GMongo;
import com.mongodb.Mongo;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;

/**
 * A Factory bean for initializing a {@link GMongo} instance
 *
 * @author Graeme Rocher
 */
public class GMongoFactoryBean implements FactoryBean<GMongo>, InitializingBean/*,
    PersistenceExceptionTranslator*/ {

    /**
     * Logger, available to subclasses.
     */
    protected final Log logger = LogFactory.getLog(getClass());

    private GMongo mongo;
    private MongoOptions mongoOptions;
    private String host;
    private Integer port;
    private List<ServerAddress> replicaSetSeeds;
    private List<ServerAddress> replicaPair;

    public void setReplicaPair(List<ServerAddress> replicaPair) {
        this.replicaPair = replicaPair;
    }

    public void setReplicaSetSeeds(List<ServerAddress> replicaSetSeeds) {
        this.replicaSetSeeds = replicaSetSeeds;
    }

    public void setMongoOptions(MongoOptions mongoOptions) {
        this.mongoOptions = mongoOptions;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public GMongo getObject() throws Exception {
        Assert.notNull(mongo, "Mongo must not be null");
        return mongo;
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
        if (mongoOptions == null) mongoOptions = new MongoOptions();
        if (replicaPair != null) {
            if (replicaPair.size() < 2) {
                throw new DatastoreConfigurationException("A replica pair must have two server entries");
            }
            mongo = new GMongo(replicaPair.get(0), replicaPair.get(1), mongoOptions);
        }
        else if (replicaSetSeeds != null) {
            mongo = new GMongo(replicaSetSeeds, mongoOptions);
        }
        else {
            String mongoHost = host != null ? host : defaultOptions.getHost();
            if (port != null) {
                mongo = new GMongo(new ServerAddress(mongoHost, port), mongoOptions);
            }
            else {
                mongo = new GMongo(mongoHost, mongoOptions);
            }
        }
    }

}
