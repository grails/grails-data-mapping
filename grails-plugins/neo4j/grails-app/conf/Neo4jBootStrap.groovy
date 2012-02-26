import org.neo4j.kernel.AbstractGraphDatabase

class Neo4jBootStrap {

    def graphDatabaseService
    def neoServer

    def init = { servletContext ->

        if (graphDatabaseService instanceof AbstractGraphDatabase) {
            try {
                neoServer = Thread.currentThread().contextClassLoader.loadClass('org.neo4j.server.WrappingNeoServerBootstrapper').newInstance([graphDatabaseService] as Object[])
                neoServer.start()
            } catch (ClassNotFoundException e) {
                log.info("org.neo4j.server.WrappingNeoServerBootstrapper not started, class not found, consider adding neo4j-server-<version>.jar to dependencies")
            }
        }
    }

    def destroy = {
        neoServer?.stop()
    }
}
