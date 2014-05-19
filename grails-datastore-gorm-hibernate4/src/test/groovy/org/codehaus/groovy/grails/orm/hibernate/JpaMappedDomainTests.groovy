package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class JpaMappedDomainTests extends AbstractGrailsHibernateTests {


    @Test
    void testJpaMappedDomain() {
        assertEquals 0, JpaAnimal.count()

        def owner = JpaOwner.newInstance(name:"Bob")
        assert owner.save(flush:true) : "should have saved owner"

        def animal = JpaAnimal.newInstance(name:"Dog", owner:owner)
        animal.save()

        assertEquals 1, JpaAnimal.count()
        assertEquals 1, JpaOwner.count()
    }

    @Test
    void testValidation() {
        def a = JpaAnimal.newInstance(name:"")
        assertNull "should not have validated", a.save(flush:true)

        assertEquals 0, JpaAnimal.count()
    }


    @Test
    void testEvents() {

        def owner = JpaOwner.newInstance(name:"Bob")
        assertNotNull "should have saved owner", owner.save(flush:true)
        assertEquals "two", JpaOwner.changeMe
    }

    @Override
    protected getDomainClasses() {
        [JpaAnimal, JpaOwner]
    }
}

@Entity
@Table(name = "animal")
class JpaAnimal {

    @Id @GeneratedValue
    int id
    Long version
    String name
    @ManyToOne
    @JoinColumn
    JpaOwner owner

    static constraints = {
        name blank:false
    }
}

@Entity
@Table(name = "owner")
class JpaOwner {
    static changeMe = "one"
    def beforeInsert() {
        JpaOwner.changeMe = "two"
    }

    @Id @GeneratedValue
    int id
    Long version
    String name
    @OneToMany
    @JoinColumn
    List<JpaAnimal> animals
}
