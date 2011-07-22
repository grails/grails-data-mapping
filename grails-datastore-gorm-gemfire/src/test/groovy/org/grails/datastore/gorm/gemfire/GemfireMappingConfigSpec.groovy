package org.grails.datastore.gorm.gemfire

import grails.gorm.tests.GormDatastoreSpec

import org.grails.datastore.mapping.gemfire.config.Region

import com.gemstone.gemfire.cache.DataPolicy

/**
 * @author graemerocher
 */
class GemfireMappingConfigSpec extends GormDatastoreSpec {

    void "Test custom region configuration"() {
        given:
            session.mappingContext.addPersistentEntity(CustomConfig)

        when:
            def entity = session.mappingContext.getPersistentEntity(CustomConfig.name)
            Region region = entity.mapping.mappedForm

        then:
            region != null
            region.dataPolicy == DataPolicy.PARTITION
            region.region == "foo"
    }
}

class CustomConfig {

    Long id
    String name

    static mapping = {
        dataPolicy DataPolicy.PARTITION
        region "foo"
    }
}
