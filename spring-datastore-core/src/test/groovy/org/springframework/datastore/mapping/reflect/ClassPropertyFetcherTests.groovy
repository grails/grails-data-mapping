package org.springframework.datastore.mapping.reflect

import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class ClassPropertyFetcherTests  {

  @Test
  void testGetProperty() {
      def cpf = ClassPropertyFetcher.forClass(Foo)

      assert 'foo' == cpf.getPropertyValue("name")
	  assert cpf.getPropertiesAssignableToType(CharSequence).size() == 1
	  assert cpf.getPropertiesAssignableToType(String).size() == 1
  }

  static class Foo {
      static String name = "foo"
	  
	  String bar
  }
}
