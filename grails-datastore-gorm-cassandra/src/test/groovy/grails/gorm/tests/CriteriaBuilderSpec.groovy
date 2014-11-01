package grails.gorm.tests


/**
 * Abstract base test for criteria queries. Subclasses should do the necessary setup to configure GORM
 */
class CriteriaBuilderSpec extends GormDatastoreSpec {

    void "Test count projection"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank"].each {
                new TestEntity(name:it, age: age++).save()
            }

            new TestEntity(name:"Chuck", age: age-1).save()
           
            def criteria = TestEntity.createCriteria()

        when:
            def result = criteria.get {
                projections {
                    count()
                }
            }

        then:
            result == 5
    }
   
    void "Test id projection"() {
        given:
            def entity = new TestEntity(name:"Bob", age: 44).save(flush:true)

        when:
            def result = TestEntity.createCriteria().get {
                projections { id() }
                idEq entity.id
            }

        then:
            result != null
            result == entity.id
    }

    void "Test idEq method"() {
        given:
            def entity = new TestEntity(name:"Bob", age: 44).save(flush:true)

        when:
            def result = TestEntity.createCriteria().get { idEq entity.id }

        then:
            result != null
            result.name == 'Bob'
    }

    void "Test disjunction query"() {
        given:
            def entity = new TestEntity(name:"Bob", age: 44).save(flush:true)
            def criteria = TestEntity.createCriteria()
        when:
            def results = criteria.list {
                or {
                    like('name', 'B%')
                    eq('age', 41)
                }
            }

        then:
            thrown UnsupportedOperationException
    }

    void "Test conjunction query"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++).save() }
            age = 40
            ["Bob", "Fred", "Barney", "Frank"].each { new PersonLastNamePartitionKey(firstName:it, lastName: 'Brown', age: age++).save() }
            
            def criteria = TestEntity.createCriteria()            

        when:
            def results = criteria.list (allowFiltering:true, max:5) {
                and {
                    eq('name', 'Bob')
                    eq('age', 40)
                }				
            }            
        then:
            1 == results.size()
        
        when:
            criteria = PersonLastNamePartitionKey.createCriteria()
            results = criteria.list {
                and {
                    eq('lastName', 'Brown')
                    eq('firstName', 'Bob')                   
                }
            }
        then:
            1 == results.size()
            
    }

    void "Test list() query"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank", "Bob"].each {
                new TestEntity(name:it, age: age++).save()
            }

            def criteria = TestEntity.createCriteria()

        when:
            def results = criteria.list {
                 eq('name', 'Bob')
            }

        then:
            2 == results.size()

        when:
            criteria = TestEntity.createCriteria()
            results = criteria.list {
                eq('name', 'Bob')
                maxResults 1
            }

        then:
            1 == results.size()
    }

    void "Test count()"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank", "Bob"].each {
                new TestEntity(name:it, age: age++).save()
            }

            def criteria = TestEntity.createCriteria()

        when:
            def result = criteria.count {
                eq('name', 'Bob')
            }

        then:
            2 == result
    }

    void "Test obtain a single result"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank"].each {
                new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
            }

            def criteria = TestEntity.createCriteria()

        when:
            def result = criteria.get {
                eq('name', 'Bob')
            }

        then:
            result != null
            "Bob" == result.name
    }

    void "Test projection unsupported"() {
        given:
            def age = 40
            ["Bob"].each {
                new TestEntity(name:it, age: age++).save()
            }

            def criteria = TestEntity.createCriteria()

        when:
            def results = criteria.list {
                like('name', 'B%')                
            }

        then:
            thrown UnsupportedOperationException
            
        when:
        criteria = TestEntity.createCriteria()
            results = criteria.list {
                projections {
                    min "age"
                }               
            }

        then:
            thrown UnsupportedOperationException
    }

    void "Test get comparison operators"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Bob"].each {
                new TestEntity(name:it, age: age++).save()
            }           

            def criteria = TestEntity.createCriteria()

        when:
            def result = criteria.get {
                projections {
                    eq('name', 'Bob')
                    lt("age", 41)
                }
                allowFiltering true				
            }

        then:
            40 == result.age

        when:
            criteria = TestEntity.createCriteria()
            result = criteria.get {
                projections {
                    eq('name', 'Bob')
                    gt("age", 42)
                }
                allowFiltering true
            }

        then:
            43 == result.age

        when:
            criteria = TestEntity.createCriteria()
            def results = criteria.list {
                projections {
                    eq('name', 'Bob')
                    between("age", 40, 43)                   
                }
                allowFiltering true
				max 5
            }         
        then:
            2 == results.size()
            results.find{ it.age == 40 }
            results.find{ it.age == 43 }            
    }

    void "Test obtain property value using projection"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank"].each {
                new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
            }

            def criteria = TestEntity.createCriteria()

        when:
            def results = criteria.list {
                projections {
                    property "age"
                }
            }

        then:
            [40, 41, 42, 43] == results.sort()
    }

    void "Test obtain association entity using property projection fail"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank"].each {
                new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
            }           

            def criteria = TestEntity.createCriteria()

        when:
            def results = criteria.list {
                projections {
                    property "child"
                }
            }

        then:
            thrown MissingPropertyException
    }
}
