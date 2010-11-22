package org.grails.datastore.gorm.riak

import org.grails.datastore.gorm.bean.factory.AbstractMappingContextFactoryBean
import org.springframework.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.springframework.datastore.mapping.model.MappingContext

/**
 * @author J. Brisbin <jon@jbrisbin.com>
 */
class RiakMappingContextFactoryBean extends AbstractMappingContextFactoryBean {

  protected MappingContext createMappingContext() { new KeyValueMappingContext("") }

}
