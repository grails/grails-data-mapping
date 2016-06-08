package org.grails.datastore.gorm.neo4j.util;

import org.grails.datastore.mapping.reflect.ClassUtils;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.neo4j.harness.internal.Ports;
import org.neo4j.server.ServerStartupException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

import static org.neo4j.graphdb.factory.GraphDatabaseSettings.BoltConnector.EncryptionLevel.DISABLED;
import static org.neo4j.graphdb.factory.GraphDatabaseSettings.boltConnector;

/**
 * Helper class for starting a Neo4j 3.x embedded server
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public class EmbeddedNeo4jServer {

    public static boolean isAvailable() {
        return ClassUtils.isPresent("org.neo4j.harness.ServerControls", EmbeddedNeo4jServer.class.getClassLoader());
    }

    /**
     * Start a server on a random free port
     *
     * @return The server controls
     */
    public static ServerControls start() throws IOException {
        return attemptStartServer(0);
    }


    /**
     * Start a server on the given address
     *
     * @param inetAddr The inet address
     *
     * @return The {@link ServerControls}
     */
    public static ServerControls start(InetSocketAddress inetAddr) {
        return start(inetAddr.getHostName(),inetAddr.getPort());
    }


    /**
     * Start a server on the given address
     *
     * @param address The address
     *
     * @return The {@link ServerControls}
     */
    public static ServerControls start(String address) {
        URI uri = URI.create(address);
        return start(new InetSocketAddress(uri.getHost(), uri.getPort()));
    }

    /**
     * Start a server on the given address
     *
     * @param host The host
     * @param port The port
     *
     * @return The {@link ServerControls}
     */
    public static ServerControls start(String host, int port) {
        String myBoltAddress = String.format("%s:%d", host, port);

        ServerControls serverControls = TestServerBuilders.newInProcessBuilder()
                .withConfig(boltConnector("0").enabled, "true")
                .withConfig(boltConnector("0").encryption_level, DISABLED.name())
                .withConfig(boltConnector("0").address, myBoltAddress)
                .newServer();

        return serverControls;
    }

    private static ServerControls attemptStartServer(int retryCount) throws IOException {

        try {
            InetSocketAddress inetAddr = Ports.findFreePort("localhost", new int[]{7687, 64 * 1024 - 1});

            ServerControls serverControls = start(inetAddr);
            return serverControls;
        } catch (ServerStartupException sse) {
            if(retryCount < 4) {
                return attemptStartServer(++retryCount);
            }
            else {
                throw sse;
            }
        }
    }
}
