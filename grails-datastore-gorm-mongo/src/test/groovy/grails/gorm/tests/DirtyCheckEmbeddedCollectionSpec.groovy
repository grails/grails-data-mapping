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
        TEST_CLASSES <<  Bar
    }

    def "Test that changes to embedded collections are detected"() {
        given:"A valid bar instance"
            def bar = createBar()
            session.clear()
        when:"The an embedded collection is modified"
            bar = Bar.get(bar.id)
            bar.strings.add("hello")
		    bar.save(flush:true)
            session.clear()
            bar = Bar.get(bar.id)
        then:"The changes are reflected correctly in the persisted instance"
            bar.strings.size() == 3
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
}

@Entity
class Bar {
	ObjectId id

	String foo
	List<String> strings = new ArrayList()

}
