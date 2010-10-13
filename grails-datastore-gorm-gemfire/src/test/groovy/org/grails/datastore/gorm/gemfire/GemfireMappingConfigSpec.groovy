package org.grails.datastore.gorm.gemfire

import grails.gorm.tests.GormDatastoreSpec
import com.gemstone.gemfire.cache.DataPolicy
import org.springframework.datastore.mapping.gemfire.config.Region

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Oct 13, 2010
 * Time: 2:12:35 PM
 * To change this template use File | Settings | File Templates.
 */
class GemfireMappingConfigSpec extends GormDatastoreSpec{

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
