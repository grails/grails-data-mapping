package org.grails.datastore.gorm.neo4j

import org.neo4j.kernel.GraphDatabaseAPI
import org.neo4j.server.configuration.Configurator
import org.neo4j.server.configuration.ServerConfigurator
import org.neo4j.server.web.WebServer

/**
 * Created by stefan on 09.06.14.
 */
class TestServer
{
    static int findFreePort() {
        ServerSocket server = new ServerSocket(0)
        int port = server.localPort
        server.close()
        port
    }

    static def startWebServer(GraphDatabaseAPI gdb) {

        def port = findFreePort()
        final ServerConfigurator config = new ServerConfigurator(gdb)
        config.configuration().setProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, port)
        def wrappingNeoServer = new TestWrappingNeoServer(gdb, config)
        def webServer = wrappingNeoServer.webServer
//            if ( auth )
//            {
//                webServer.addFilter( new TestAuthenticationFilter(), "/*" );
//            }
        wrappingNeoServer.start()
        [port, webServer]
//        return webServer
    }
}
