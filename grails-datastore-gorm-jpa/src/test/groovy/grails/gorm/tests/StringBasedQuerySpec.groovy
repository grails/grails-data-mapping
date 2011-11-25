package grails.gorm.tests

class StringBasedQuerySpec extends GormDatastoreSpec {
    @Override
    List getDomainClasses() {
        [TestEntity]
    }


    void "Test findAll method that takes a JPQL string"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }

        when:
            def people = TestEntity.findAll("select te from TestEntity as te")

        then:
            people.size() == 4

        when:
            people = TestEntity.findAll("select te from TestEntity as te", [max:2])

        then:
            people.size() == 2

        when:
            people = TestEntity.findAll("select te from TestEntity as te where te.name like ?1", ["B%"])

        then:
            people.size() == 2

        when:
            people = TestEntity.findAll("select te from TestEntity as te where te.name like :name", [name:"B%"])

        then:
            people.size() == 2

    }

    void "Test find method that takes a JPQL string"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }

        when:
            def person = TestEntity.find("select te from TestEntity as te")

        then:
            person != null

        when:
            person = TestEntity.find("select te from TestEntity as te where te.name = ?1", ["Bob"])

        then:
            person != null
            person.name == "Bob"

        when:
            person = TestEntity.find("select te from TestEntity as te where te.name = :name", [name:"Frank"])

        then:
            person != null
            person.name == "Frank"
    }
}
