package grails.gorm.tests

import org.grails.datastore.mapping.core.Session

/**
 * Created by Jim on 8/12/2016.
 */
class ConfigGormDatastoreSpec extends GormDatastoreSpec {

    @Override
    Session createSession() {
        setupClass.setup(((TEST_CLASSES + getDomainClasses()) as Set) as List, new ConfigObject(['hibernate': ['hbm2ddl.auto': 'update']]))
    }

}
