package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test
import static junit.framework.Assert.*

class FindMethodTests extends AbstractGrailsHibernateTests {

    @Test
    void testFindMethodWithHQL() {
        assertNotNull "should have saved", FindMethodTestClass.newInstance(one:"one", two:2).save(flush:true)

        session.clear()

        assert FindMethodTestClass.find("from FindMethodTestClass as f where f.one = ? and f.two = ?", ["one", 2]) : "should have returned a result"
    }

    @Test
    void testFindMethodWithHQLUpperCase() {
        assertNotNull "should have saved", FindMethodTestClass.newInstance(one:"one", two:2).save(flush:true)

        session.clear()

        assert FindMethodTestClass.find("FROM FindMethodTestClass as f where f.one = ? and f.two = ?", ["one", 2]) : "should have returned a result"
    }

    @Test
    void testFindMethodWithHQLWithMultiline() {
        assertNotNull "should have saved", FindMethodTestClass.newInstance(one:"one", two:2).save(flush:true)

        session.clear()

        assert FindMethodTestClass.find("""
                                    FROM FindMethodTestClass as f 
                                    where f.one = ? 
                                    and f.two = ?
                                    """, ["one", 2]) : "should have returned a result"
    }

    @Test
    void testUsingHibernateCache() {
        def stats = sessionFactory.statistics
        stats.statisticsEnabled = true
        stats.clear()

        def cacheStats = stats.getSecondLevelCacheStatistics('org.hibernate.cache.internal.StandardQueryCache')
        assertEquals 0, cacheStats.hitCount
        assertEquals 0, cacheStats.missCount
        assertEquals 0, cacheStats.putCount

        FindMethodTestClass.find("from FindMethodTestClass where one = 'Angus'", [cache: true])
        assertEquals 0, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        FindMethodTestClass.find("from FindMethodTestClass where one = 'Angus'", [cache: true])
        assertEquals 1, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        FindMethodTestClass.find("from FindMethodTestClass where one = 'Angus'", [cache: true])
        assertEquals 2, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        FindMethodTestClass.find("from FindMethodTestClass where one = 'Angus'")
        assertEquals 2, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        FindMethodTestClass.find("from FindMethodTestClass where one = 'Angus'", [cache: false])
        assertEquals 2, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        FindMethodTestClass.find("from FindMethodTestClass where one = 'Angus'", [cache: true])
        assertEquals 3, cacheStats.hitCount
        assertEquals 1, cacheStats.missCount
        assertEquals 1, cacheStats.putCount

        FindMethodTestClass.find("from FindMethodTestClass where one = 'Malcolm'", [cache: true])
        assertEquals 3, cacheStats.hitCount
        assertEquals 2, cacheStats.missCount
        assertEquals 2, cacheStats.putCount

        FindMethodTestClass.find("from FindMethodTestClass where one = 'Malcolm'", [cache: true])
        assertEquals 4, cacheStats.hitCount
        assertEquals 2, cacheStats.missCount
        assertEquals 2, cacheStats.putCount

        FindMethodTestClass.find("from FindMethodTestClass where one = :name", [name: 'Brian'], [cache: true])
        assertEquals 4, cacheStats.hitCount
        assertEquals 3, cacheStats.missCount
        assertEquals 3, cacheStats.putCount

        FindMethodTestClass.find("from FindMethodTestClass where one = :name", [name: 'Brian'], [cache: true])
        assertEquals 5, cacheStats.hitCount
        assertEquals 3, cacheStats.missCount
        assertEquals 3, cacheStats.putCount

        FindMethodTestClass.find("from FindMethodTestClass where one = :name", [name: 'Bon'], [cache: true])
        assertEquals 5, cacheStats.hitCount
        assertEquals 4, cacheStats.missCount
        assertEquals 4, cacheStats.putCount

        FindMethodTestClass.find("from FindMethodTestClass where one = :name", [name: 'Bon'], [cache: false])
        assertEquals 5, cacheStats.hitCount
        assertEquals 4, cacheStats.missCount
        assertEquals 4, cacheStats.putCount

        FindMethodTestClass.find("from FindMethodTestClass where one = :name", [name: 'Bon'], [cache: true])
        assertEquals 6, cacheStats.hitCount
        assertEquals 4, cacheStats.missCount
        assertEquals 4, cacheStats.putCount
    }

    @Override
    protected getDomainClasses() {
        [FindMethodTestClass]
    }
}

@Entity
class FindMethodTestClass {
    Long id
    Long version

    String one
    Integer two
}
