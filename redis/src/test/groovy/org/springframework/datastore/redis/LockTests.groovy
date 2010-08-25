package org.springframework.datastore.redis

import org.junit.Test

/**
 * Tests for locking
 */
class LockTests {

  @Test
  void testLock() {
    return // not yet implemented
    def ds = new RedisDatastore()
    ds.mappingContext.addPersistentEntity(Candidate)
    def session = ds.connect()
    session.getNativeInterface().flushdb()

    def c = new Candidate()
    session.persist(c)
    session.flush()

    int x = 5
    int y = 3
    def threads = []
    def s2 = ds.connect()
    x.times {
      threads << Thread.start {
        y.times {

          def c2 = s2.lock(Candidate, c.id)
          c2.votes++
          s2.persist(c2)
          s2.flush()
          s2.unlock c2
        }
      }
    }

    threads.each { Thread t -> t.join() }
    
    c = session.retrieve(Candidate, c.id)

    assert x * y == c.votes

  }
}

class Candidate {
  Long id
  String name
  int votes
}
