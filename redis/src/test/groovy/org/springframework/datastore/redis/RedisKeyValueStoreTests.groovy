package org.springframework.datastore.redis

import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class RedisKeyValueStoreTests {

  @Test
  void testReadWrite() {
    // NOTE: test requires a running instance of Redis
    // need to figure out a way to Mock
    
    def ds = new RedisDatastore()

    def conn = ds.connect(null)
    conn.clear()

    assert 0 == conn.size()

    conn["foo"] = "bar"

    assert 1 == conn.size()
    assert !conn.isEmpty()
    
    assert "bar" == conn["foo"]

    conn.remove("foo")

    assert null == conn["foo"]
    assert 0 == conn.size()
    assert conn.isEmpty()
  }
}
