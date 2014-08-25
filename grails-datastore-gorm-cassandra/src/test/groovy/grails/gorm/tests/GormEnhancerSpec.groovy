package grails.gorm.tests

import spock.lang.Ignore

/**
 * @author graemerocher
 */
class GormEnhancerSpec extends GormDatastoreSpec {

    void "Test basic CRUD operations"() {
        given:
            def t

        when:
            t = TestEntity.get(1)

        then:
            t == null

        when:
            t = new TestEntity(name:"Bob", child:new ChildEntity(name:"Child"))
            t.save()

        then:
            t.id != null

        when:
            def results = TestEntity.list()

        then:
            1 == results.size()
            "Bob" == results[0].name

        when:
            t = TestEntity.get(t.id)

        then:
            t != null
            "Bob" == t.name
    }

    void "Test simple dynamic finder"() {

        given:
            def t = new TestEntity(name:"Bob", child:new ChildEntity(name:"Child"))
            t.save()

            t = new TestEntity(name:"Fred", child:new ChildEntity(name:"Child"))
            t.save()

        when:
            def results = TestEntity.list()
            def bob = TestEntity.findByName("Bob")

        then:
            2 == results.size()
            bob != null
            "Bob" == bob.name
    }

    void "Test dynamic finder with disjunction"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney"].each {
                new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
            }

        when:
            def results = TestEntity.findAllByNameOrAge("Barney", 40)
        then:
            thrown UnsupportedOperationException
    }

    void "Test getAll() method"() {
        given:
            def age = 40
            def ids = []
            ["Bob", "Fred", "Barney"].each {
                ids.add(new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save().id)
            }

        when:
            def results = TestEntity.getAll(ids[0],ids[1])

        then:
            2 == results.size()
    }

    void "Test ident() method"() {
        given:
            def t

        when:
            t = new TestEntity(name:"Bob", child:new ChildEntity(name:"Child"))
            t.save()

        then:
            t.id != null
            t.id == t.ident()
    }

    void "Test dynamic finder with pagination parameters"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank", "Barney"].each {
                new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
            }

        when:
            def total = TestEntity.count()

        then:
            5 == total
			
			1 == TestEntity.findAllByAge(40).size()
            2 == TestEntity.findAllByName("Barney").size()
            1 == TestEntity.findAllByName("Barney", [max:1]).size()
    }

    void "Test in list query"() {
        given:
           
			["IPhone", "Samsung", "LG", "HTC", "Nokia", "Blackberry"].each {
				new SimpleWidget(category: "phone", name:it).save()
			}

        when:
            def total = SimpleWidget.count()

        then:
            6 == total
            2 == SimpleWidget.findAllByNameInList(["IPhone", "Samsung"], [allowFiltering:true]).size()
            1 == SimpleWidget.findAllByNameInList(["None", "Blackberry"], [allowFiltering:true]).size()
            0 == SimpleWidget.findAllByNameInList(["None1", "None2"], [allowFiltering:true]).size()
            1 == SimpleWidget.findAllByNameInListAndCategory(["None", "LG"], "phone").size()
    }

    void "Test like query"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank", "frita"].each {
                new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
            }

        when:
            def results = TestEntity.findAllByNameLike("Fr%")

        then:
            thrown UnsupportedOperationException
    }

    void "Test ilike query"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank", "frita"].each {
                new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
            }

        when:
            def results = TestEntity.findAllByNameIlike("fr%")

        then:
            thrown UnsupportedOperationException
    }

    void "Test count by query"() {

        given:
            def age = 40
            ["Bob", "Fred", "Barney"].each {
                new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
            }

        when:
            def total = TestEntity.count()

        then:
            3 == total
            3 == TestEntity.list().size()
            1 == TestEntity.countByName("Barney")
            1 == TestEntity.countByNameAndAge("Bob", 40, [allowFiltering:true])
    }

    void "Test dynamic finder with conjunction"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney"].each {
                new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
            }

        when:
            def total = TestEntity.count()

        then:
            3 == total
            3 == TestEntity.list().size()

            TestEntity.findByNameAndAge("Bob", 40, [allowFiltering:true])
            !TestEntity.findByNameAndAge("Bob", 41, [allowFiltering:true])
    }

    void "Test count() method"() {
        given:
            def t

        when:
            t= new TestEntity(name:"Bob", child:new ChildEntity(name:"Child"))
            t.save()

        then:
            1 == TestEntity.count()

        when:
            t = new TestEntity(name:"Fred", child:new ChildEntity(name:"Child"))
            t.save()

        then:
            2 == TestEntity.count()
    }
}
