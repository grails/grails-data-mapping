package org.grails.datastore.mapping.redis

import org.junit.After
import org.junit.Test
import org.grails.datastore.mapping.core.Session

/**
 * Tests for locking
 */
class LockTests extends AbstractRedisTest {

    private Session s2

    @Test
    void testLock() {
        return // not yet implemented
        ds.mappingContext.addPersistentEntity(Candidate)
        session.getNativeInterface().flushdb()

        def c = new Candidate()
        session.persist(c)
        session.flush()

        int x = 5
        int y = 3
        def threads = []
        s2 = ds.connect()
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

    @After
    void after() {
        s2?.disconnect()
    }
}

class Candidate {
    Long id
    String name
    int votes
}
