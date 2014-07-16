grails {
    neo4j {

        // neo4j jdbc url, see https://github.com/neo4j-contrib/neo4j-jdbc for syntax
        // this configures a embedded instance
        url = "jdbc:neo4j:instance:dummy"

        // for remote usage:
        // url = "jdbc:neo4j://localhost:7474/"
        // optional: if authentication extension is used on Neo4j server, provide credentials:
        //    username = "neo4j"
        //    password = "<mypasswd>"

        // any other stuff below is just for embedded instances

        // configure embedded mode, if true HA is used, false refers to single instance
        // NB: for HA you need to configure dbProperties
        // NB: for HA you need to add dependencies
        // ha = false

        // set graph.db location, defaults to data/graph.db
        // location = "<path for graph.db>"

        // put any Neo4j config options (normally residing in neo4j.properties) here
        // NB: this does of course not work if url is a remote connection
        dbProperties = [

            // allow_store_upgrade: true,
            // remote_shell_enabled: true,

            // minimum required sample settings for ha=true
            // see http://docs.neo4j.org/chunked/stable/ha-configuration.html#_database_configuration for full list
            // 'ha.server_id': 1,
            // 'ha.initial_hosts': "localhost:5001-5003",
        ]
    }
}

// environment specific settings
environments {
    development {
        dataSource {
            dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
            url = "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }
    }
    test {
        dataSource {
            dbCreate = "update"
            url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }
    }
    production {
        dataSource {
            dbCreate = "update"
            url = "jdbc:h2:prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
            properties {
               maxActive = -1
               minEvictableIdleTimeMillis=1800000
               timeBetweenEvictionRunsMillis=1800000
               numTestsPerEvictionRun=3
               testOnBorrow=true
               testWhileIdle=true
               testOnReturn=false
               validationQuery="SELECT 1"
               jdbcInterceptors="ConnectionState"
            }
        }
    }
}
