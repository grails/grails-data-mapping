package org.grails.orm.hibernate.connections

import org.grails.datastore.mapping.core.DatastoreUtils
import org.hibernate.dialect.Oracle8iDialect
import spock.lang.Specification

/**
 * Created by graemerocher on 05/07/16.
 */
class HibernateConnectionSourceSettingsSpec extends Specification {

    void "test hibernate connection source settings"() {
        when:"The configuration is built"
        Map config = [
                'dataSource.dbCreate': 'update',
                'dataSource.dialect': Oracle8iDialect.name,
                'dataSource.formatSql': 'true',
                'hibernate.flush.mode': 'COMMIT',
                'hibernate.cache.queries': 'true',
                'hibernate.hbm2ddl.auto': 'create'
        ]
        HibernateConnectionSourceSettingsBuilder builder = new HibernateConnectionSourceSettingsBuilder(DatastoreUtils.createPropertyResolver(config))
        HibernateConnectionSourceSettings settings = builder.build()

        def expectedDataSourceProperties = new Properties()
        expectedDataSourceProperties.put('hibernate.hbm2ddl.auto', 'update')
        expectedDataSourceProperties.put('hibernate.show_sql', 'false')
        expectedDataSourceProperties.put('hibernate.format_sql', 'true')
        expectedDataSourceProperties.put('hibernate.dialect', Oracle8iDialect.name)

        def expectedHibernateProperties = new Properties()
        expectedHibernateProperties.put('hibernate.hbm2ddl.auto', 'create')
        expectedHibernateProperties.put('hibernate.cache.queries', 'true')
        expectedHibernateProperties.put('hibernate.flush.mode', 'COMMIT')

        def expectedCombinedProperties = new Properties()
        expectedCombinedProperties.putAll(expectedDataSourceProperties)
        expectedCombinedProperties.putAll(expectedHibernateProperties)

        then:"The results are correct"
        settings.dataSource.dbCreate == 'update'
        settings.dataSource.dialect == Oracle8iDialect
        settings.dataSource.formatSql
        !settings.dataSource.logSql
        settings.dataSource.toHibernateProperties() == expectedDataSourceProperties

        settings.hibernate.getFlush().mode == HibernateConnectionSourceSettings.HibernateSettings.FlushSettings.FlushMode.COMMIT
        settings.hibernate.getCache().queries
        settings.hibernate.get('hbm2ddl.auto') == 'create'
        settings.hibernate.toProperties() == expectedHibernateProperties
    }
}
