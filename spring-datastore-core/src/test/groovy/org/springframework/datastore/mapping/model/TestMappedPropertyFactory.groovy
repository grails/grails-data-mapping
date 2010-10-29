package org.springframework.datastore.mapping.model

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class TestMappedPropertyFactory extends MappingFactory {

    def createMappedForm(PersistentProperty mpp) {
      return "${mpp.name}_mapped"
    }

    def createMappedForm(PersistentEntity entity) {
      return "${entity.name}_mapped"
    }
}
