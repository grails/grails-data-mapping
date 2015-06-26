package grails.test.mixin.hibernate;

public class Junit3SubclassTest extends Junit3BaseclassTest {
    void setUp() {
        super.setUp()
        def person = new Person(name:'John Doe')
        def personId = person.save(flush:true, failOnError:true)?.id
    }
    
    void testThatGormIsAvailableInSubclass() {
        assert Person.count() == 1
    }
}
