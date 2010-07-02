package org.springframework.datastore.mapping

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class TestMappedPropertyFactory<String, GString> extends MappingFactory {

    String createMappedForm(PersistentProperty mpp) {
      return "${mpp.name}_mapped"
    }

    String createMappedForm(PersistentEntity entity) {
      return "${entity.name}_mapped"
    }
}
