package grails.gorm.tests

import org.bson.types.ObjectId
import grails.persistence.Entity

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 10/11/11
 * Time: 2:26 PM
 * To change this template use File | Settings | File Templates.
 */
class DirtyCheckEmbeddedCollectionSpec extends GormDatastoreSpec {

    static {
        TEST_CLASSES << Foo << Bar
    }

    def "Test that changes to basic collections are detected"() {
        given:"A valid bar instance"
            def bar = createBar()
            session.clear()
        when:"A basic collection is modified"
            bar = Bar.get(bar.id)
            bar.strings.add("hello")
		    bar.save(flush:true)
            session.clear()
            bar = Bar.get(bar.id)
        then:"The changes are reflected correctly in the persisted instance"
            bar.strings.size() == 3

         when:"A basic collection is cleared"
            bar.strings.clear()
            bar.save(flush:true)
            session.clear()
            bar = Bar.get(bar.id)

        then:"The collection is empty"
            bar.strings.size() == 0

    }

    def "Test that an embedded collection can be cleared"() {
        given:"valid foo and bar instances"
            def foo = createFooBar()
            session.clear()


        when:"foo is looked up"
            foo == Foo.get(foo.id)

        then:"It has 1 bar"
            foo.bars.size() == 1


        when:"The collection is cleared"
            foo.bars.clear()
            foo.save(flush:true)
            session.clear()

            foo == Foo.get(foo.id)
        then:"The collection is empty on nexted lookup"
            foo.bars.size() == 0


    }

    protected def createBar() {
        Bar bar = new Bar(foo: 'foo')
        bar.strings.add("test")
        bar.save()
        //bar is correctly saved
        bar = Bar.get(bar.id)
        bar.strings.add("test2")
        bar.save(flush:true)
    }

    protected createFooBar() {
	    def bar = new Bar(foo:"test")
		def foo = new Foo(testProperty:"test")
		foo.bars.add(bar)
		foo.save(flush:true)
    }
}

@Entity
class Foo {
	ObjectId id
	String testProperty
    Set bars = []
	static hasMany = [bars:Bar]
	static embedded = ['bars']
}
@Entity
class Bar {
	ObjectId id

	String foo
	List<String> strings = new ArrayList()

}
