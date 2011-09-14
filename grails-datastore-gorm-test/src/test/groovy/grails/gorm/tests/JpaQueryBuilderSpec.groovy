package grails.gorm.tests

import grails.gorm.DetachedCriteria
import org.grails.datastore.mapping.query.jpa.JpaQueryBuilder
import org.grails.datastore.mapping.query.Query.Conjunction
import org.springframework.dao.InvalidDataAccessResourceUsageException

/**
 * Test for JPA builder
 */
class JpaQueryBuilderSpec extends GormDatastoreSpec{

    void "Test exception is thrown in join with delete"() {
        given:"Some criteria"
            DetachedCriteria criteria = new DetachedCriteria(Person).build {
                pets {
                    eq 'name', 'Ted'
                }
                eq 'firstName', 'Bob'
            }

        when:"A jpa query is built"
            def builder = new JpaQueryBuilder(session.mappingContext.getPersistentEntity(Person.name),criteria.criteria)
            builder.buildDelete()

        then:"The query is valid"
            def e = thrown(InvalidDataAccessResourceUsageException)
            e.message == 'Joins cannot be used in a DELETE or UPDATE operation'

    }

    void "Test build update property natural ordering"() {
        given:"Some criteria"
            DetachedCriteria criteria = new DetachedCriteria(Person).build {
                eq 'firstName', 'Bob'
            }

        when:"A jpa query is built"
            def builder = new JpaQueryBuilder(session.mappingContext.getPersistentEntity(Person.name),criteria.criteria)
            def queryInfo = builder.buildUpdate(firstName:'Bob updated', age:30)


        then:"The query is valid"
            queryInfo.query != null
            queryInfo.query == 'UPDATE grails.gorm.tests.Person person SET person.age=?1, person.firstName=?2 WHERE (person.firstName=?3)'
            queryInfo.parameters == [30,"Bob updated", "Bob"]
    }

    void "Test build update"() {
        given:"Some criteria"
            DetachedCriteria criteria = new DetachedCriteria(Person).build {
                eq 'firstName', 'Bob'
            }

        when:"A jpa query is built"
            def builder = new JpaQueryBuilder(session.mappingContext.getPersistentEntity(Person.name),criteria.criteria)
            def queryInfo = builder.buildUpdate(age:30)


        then:"The query is valid"
            queryInfo.query != null
            queryInfo.query == 'UPDATE grails.gorm.tests.Person person SET person.age=?1 WHERE (person.firstName=?2)'
            queryInfo.parameters == [30, "Bob"]
    }

    void "Test build delete"() {
        given:"Some criteria"
            DetachedCriteria criteria = new DetachedCriteria(Person).build {
                eq 'firstName', 'Bob'
            }

        when:"A jpa query is built"
            def builder = new JpaQueryBuilder(session.mappingContext.getPersistentEntity(Person.name),criteria.criteria)
            def queryInfo = builder.buildDelete()


        then:"The query is valid"
            queryInfo.query != null
            queryInfo.query == 'DELETE grails.gorm.tests.Person person WHERE (person.firstName=?1)'
            queryInfo.parameters == ["Bob"]
    }

    void "Test build simple select"() {
        given:"Some criteria"
            DetachedCriteria criteria = new DetachedCriteria(Person).build {
                eq 'firstName', 'Bob'
            }

        when:"A jpa query is built"
            def builder = new JpaQueryBuilder(session.mappingContext.getPersistentEntity(Person.name),criteria.criteria)
            def query = builder.buildSelect().query


        then:"The query is valid"
            query != null
            query == 'SELECT DISTINCT person FROM grails.gorm.tests.Person AS person WHERE (person.firstName=?1)'
    }

    void "Test build select with or"() {
        given:"Some criteria"
            DetachedCriteria criteria = new DetachedCriteria(Person).build {
                or {
                    eq 'firstName', 'Bob'
                    eq 'firstName', 'Fred'
                }

            }

        when:"A jpa query is built"
            def builder = new JpaQueryBuilder(session.mappingContext.getPersistentEntity(Person.name),criteria.criteria)
            final queryInfo = builder.buildSelect()


        then:"The query is valid"
            queryInfo.query!= null
            queryInfo.query == 'SELECT DISTINCT person FROM grails.gorm.tests.Person AS person WHERE ((person.firstName=?1 OR person.firstName=?2))'
            queryInfo.parameters == ['Bob', 'Fred']

    }
}
