package grails.gorm.tests


import org.grails.datastore.gorm.proxy.GroovyProxyFactory

import spock.lang.Ignore

@Ignore("Cassandra GORM does not support associations at present")
class UpdateWithProxyPresentSpec extends GormDatastoreSpec {

    void "Test update entity with association proxies"() {
        given:
            session.mappingContext.setProxyFactory(new GroovyProxyFactory())
            def person = new Person(firstName:"Bob", lastName:"Builder")
            def petType = new PetType(name:"snake")
            def pet = new Pet(name:"Fred", type:petType, owner:person)
            person.addToPets(pet)
            person.save(flush:true)
            session.clear()

        when:
            person = Person.get(person.id)
            person.firstName = "changed"
            person.save(flush:true)
            session.clear()
            person = Person.get(person.id)
            def personPet = person.pets.iterator().next()

        then:
            person.firstName == "changed"
            personPet.name == "Fred"
            personPet.id == pet.id
            personPet.owner.id == person.id
            personPet.type.name == 'snake'
            personPet.type.id == petType.id
    }

    void "Test update unidirectional oneToMany with proxy"() {
        given:
        session.mappingContext.setProxyFactory(new GroovyProxyFactory())
        def parent = new Parent(name: "Bob").save(flush: true)
        def child = new Child(name: "Bill").save(flush: true)
        session.clear()

        when:
        parent = Parent.get(parent.id)
        child = Child.load(child.id) // make sure we've got a proxy.
        parent.addToChildren(child)
        parent.save(flush: true)
        session.clear()
        parent = Parent.get(parent.id)

        then:
        parent.name == 'Bob'
        parent.children.size() == 1

        when:
        child = parent.children.first()

        then:
        child.name == "Bill"
    }
}


