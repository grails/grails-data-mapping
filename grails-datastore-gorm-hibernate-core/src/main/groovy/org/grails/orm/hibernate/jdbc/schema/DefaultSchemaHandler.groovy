package org.grails.orm.hibernate.jdbc.schema

import javax.sql.DataSource
import java.sql.Connection
import java.sql.ResultSet

/**
 * Resolves the schema names
 *
 * @author Graeme Rocher
 * @since 6.0
 */
class DefaultSchemaHandler implements SchemaHandler {

    final String useSchemaStatement
    final String createSchemaStatement
    final String defaultSchemaName

    DefaultSchemaHandler() {
        useSchemaStatement = "SET SCHEMA {0}"
        createSchemaStatement = "CREATE SCHEMA {0}"
        defaultSchemaName = "PUBLIC"
    }

    DefaultSchemaHandler(String useSchemaStatement, String createSchemaStatement, String defaultSchemaName) {
        this.useSchemaStatement = useSchemaStatement
        this.createSchemaStatement = createSchemaStatement
        this.defaultSchemaName = defaultSchemaName
    }

    @Override
    void useSchema(Connection connection, String name) {
        connection
                .createStatement()
                .execute( String.format(useSchemaStatement, name) )
    }

    @Override
    void useDefaultSchema(Connection connection) {
        useSchema(connection, defaultSchemaName)
    }

    @Override
    void createSchema(Connection connection, String name) {
        connection
                .createStatement()
                .execute( String.format(createSchemaStatement, name) )
    }

    @Override
    Collection<String> resolveSchemaNames(DataSource dataSource) {
        Collection<String> schemaNames = []
        Connection connection
        try {
            connection = dataSource.getConnection()
            ResultSet schemas = connection.getMetaData().getSchemas()
            while(schemas.next()) {
                schemaNames.add(schemas.getString("TABLE_SCHEM"))
            }
        } finally {
            connection?.close()
        }
        return schemaNames
    }
}
