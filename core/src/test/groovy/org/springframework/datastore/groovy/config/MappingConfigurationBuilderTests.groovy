package org.springframework.datastore.groovy.config

import org.junit.Test
import org.springframework.datastore.keyvalue.mapping.Family
import org.springframework.datastore.keyvalue.mapping.KeyValue
import org.springframework.datastore.config.groovy.MappingConfigurationBuilder

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class MappingConfigurationBuilderTests {

  @Test
  void testCast() {
    println( [family:"foo"] as Family )
  }
  @Test
  void testParseKeyValueSettings() {
    def f = new Family("test", "test")
    def builder = new MappingConfigurationBuilder(f, KeyValue)
    def callable = {
       family "foo"
       keyspace "bar"

       test2 key:"two", indexed:true
    }

    callable.delegate = builder
    callable.resolveStrategy = Closure.DELEGATE_ONLY

    callable.call()

    assert "foo" == f.family
    assert "bar" == f.keyspace
    KeyValue kv = builder.properties.test2
    assert 'two', kv.key
    assert kv.indexed
  }
}
