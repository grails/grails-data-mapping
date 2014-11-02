package grails.gorm.tests

import spock.lang.Ignore

/**
 * Abstract base test for loading proxies. Subclasses should do the necessary setup to configure GORM
 */
class ProxyLoadingSpec extends GormDatastoreSpec {

    void "Test load proxied instance directly"() {

        given:
            def t = new TestEntity(name:"Bob", age: 45, child:new ChildEntity(name:"Test Child")).save(flush:true)
			def person = new PersonLastNamePartitionKey(firstName: 'Jake', lastName: 'Brown', age: 35).save(flush:true)
			session.clear()
        when:
            def proxy = TestEntity.load(t.id)
			def proxyPerson = PersonLastNamePartitionKey.load([firstName: person.firstName, lastName: person.lastName, age: person.age])
        then:
            proxy != null
            t.id == proxy.id
            "Bob" == proxy.name
			session.getCachedInstance(TestEntity, t.id)
			
			proxyPerson != null
			'Jake' == proxyPerson.firstName
			'Brown' == proxyPerson.lastName
			35 == proxyPerson.age
			session.getCachedInstance(PersonLastNamePartitionKey, [firstName: person.firstName, lastName: person.lastName, age: person.age])
    }

    @Ignore("Cassandra GORM does not support associations at present")
    void "Test query using proxied association"() {
        given:
            def child = new ChildEntity(name: "Test Child")
            def t = new TestEntity(name:"Bob", age: 45, child:child).save()

        when:
            def proxy = ChildEntity.load(child.id)
            t = TestEntity.findByChild(proxy)

        then:
            t != null
    }
}
