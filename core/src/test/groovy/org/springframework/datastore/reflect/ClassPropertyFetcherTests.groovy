package org.springframework.datastore.reflect

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
  }

  static class Foo {
      static String name = "foo"
  }
}
