package org.grails.orm.hibernate

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Oct 27, 2008
 */
class VersionColumnTests extends AbstractGrailsHibernateTests {


    @Test
    void testVersionColumnMapping() {
        // will fail if the column is not mapped correctly
        session.connection().prepareStatement("select v_number from version_column_book").execute()
    }

    @Test
    void testLongVersion() {
        assertEquals Long, ga.getDomainClass(BigLongVersion.name).version.type
    }

    @Test
    void testDateVersion() {
        assertEquals Date, ga.getDomainClass(DateVersion.name).version.type
    }

    @Override
    protected getDomainClasses() {
        [VersionColumnBook, DateVersion, BigLongVersion]
    }
}

class VersionColumnBook {
    Long id
    Long version
    String title

    static mapping = {
        version 'v_number'
    }
}


class BigLongVersion {
    Long id
    Long version
    String name
}