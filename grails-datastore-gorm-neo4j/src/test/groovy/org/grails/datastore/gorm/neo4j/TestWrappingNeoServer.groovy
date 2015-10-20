package org.grails.datastore.gorm.neo4j

import org.neo4j.kernel.GraphDatabaseAPI
import org.neo4j.kernel.configuration.Config
import org.neo4j.server.WrappingNeoServer
import org.neo4j.server.configuration.Configurator
import org.neo4j.server.modules.DBMSModule

//import org.neo4j.server.modules.DiscoveryModule
import org.neo4j.server.modules.RESTApiModule
import org.neo4j.server.modules.ServerModule
import org.neo4j.server.modules.ThirdPartyJAXRSModule

/**
 * Created by stefan on 09.06.14.
 */
class TestWrappingNeoServer extends WrappingNeoServer {

    TestWrappingNeoServer(GraphDatabaseAPI db, Configurator config) {
        super(db, config)
    }

    @Override
    protected Iterable<ServerModule> createServerModules() {
        [
                new DBMSModule(webServer),
                new RESTApiModule(
                        webServer,
                        database,
                        config,
                        logging
                ),
                new ThirdPartyJAXRSModule(
                        webServer,
                        config,
                        logging,
                        this
                )
        ]
    }
}
