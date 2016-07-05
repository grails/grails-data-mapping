package org.grails.orm.hibernate.connections

import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.orm.hibernate.jdbc.connections.DataSourceConnectionSourceFactory
import org.grails.orm.hibernate.jdbc.connections.DataSourceSettings
import org.hibernate.dialect.Oracle8iDialect
import spock.lang.Specification

import javax.sql.DataSource

/**
 * Created by graemerocher on 05/07/16.
 */
class DataSourceConnectionSourceFactorySpec extends Specification {

    void "test datasource connection source factory"() {
        when:
        DataSourceConnectionSourceFactory factory = new DataSourceConnectionSourceFactory()
        Map config = [
                'dataSource.url':"jdbc:h2:mem:grailsDB;MVCC=TRUE;LOCK_TIMEOUT=10000",
                'dataSource.dbCreate': 'update',
                'dataSource.dialect': Oracle8iDialect.name
        ]
        def connectionSource = factory.create(ConnectionSource.DEFAULT, DatastoreUtils.createPropertyResolver(config))

        then:"The connection source is correct"
        connectionSource.name == ConnectionSource.DEFAULT
        connectionSource.source
    }
}
