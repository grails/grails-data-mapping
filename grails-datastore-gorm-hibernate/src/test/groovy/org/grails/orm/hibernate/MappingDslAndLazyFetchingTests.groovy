package org.grails.orm.hibernate

import grails.persistence.Entity;

import org.hibernate.Hibernate

import static junit.framework.Assert.*

import org.junit.Test

/**
 * test for GRAILS-2923.
 *
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 10, 2008
 */
class MappingDslAndLazyFetchingTests extends AbstractGrailsHibernateTests {
    @Override
    protected getDomainClasses() {
        [Actor, Film]
    }

    // test for GRAILS-2923
    @Test
    void testMappingDslAndLazyFetching() {
        def a = Actor.newInstance(firstName:"Edward", lastName:"Norton")

        def f = Film.newInstance(name:"American History X")
        f.addToActors(a)
        assertNotNull f.save(flush:true)

        session.clear()

        f = Film.get(1)
        assertFalse "lazy loading should be the default", Hibernate.isInitialized(f.actors)
    }
}

@Entity
class Actor {

    Long id
    Long version

    static belongsTo = Film
    Set films
    static hasMany = [films:Film]

    String firstName
    String lastName

    static mapping = {
        id column:'actor_id'
        films column:'film_id'
    }
}

@Entity
class Film {
    Long id
    Long version

    String name
    Set actors
    static hasMany = [actors:Actor]

    static mapping = {
        actors column:'actor_id'
    }
}