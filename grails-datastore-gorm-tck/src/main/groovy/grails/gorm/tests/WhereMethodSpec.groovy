package grails.gorm.tests

import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 9/6/11
 * Time: 3:41 PM
 * To change this template use File | Settings | File Templates.
 */
@ApplyDetachedCriteriaTransform
class WhereMethodSpec extends GormDatastoreSpec {

    def "Test nested and/or query"() {
        given:"A bunch of people"
            createPeople()

        when: "A where query is used"
            def query = Person.where {
                (lastName != "Simpson" && firstName != "Fred") || firstName == "Bart"
            }
            def results = query.list(sort:"firstName")

        then:"The correct result is returned"
            results.size() == 2
            results[0].firstName == "Barney"
            results[1].firstName == "Bart"
    }
    def "Test not equal query"() {
        given:"A bunch of people"
            createPeople()

        when: "A where query is used"
            def query = Person.where {
                 lastName != "Simpson"
            }
            def results = query.list(sort:"firstName")

        then:"The correct result is returned"
            results.size() == 2
            results[0].firstName == "Barney"
            results[1].firstName == "Fred"
    }
    def "Test basic binary criterion where call"() {
        given:"A bunch of people"
            createPeople()

        when: "A where query is used"
            def query = Person.where {
                 firstName == "Bart" && lastName == "Simpson"
            }
            def result = query.get()

        then:"The correct result is returned"

            result != null
            result.firstName == "Bart"

    }

    def "Test basic single criterion where call"() {
        given:"A bunch of people"
            createPeople()

        when: "A where query is used"
            def query = Person.where {
               firstName == "Bart"
            }
            def result = query.get()

        then:"The correct result is returned"

            result != null
            result.firstName == "Bart"

    }

    protected def createPeople() {
        new Person(firstName: "Homer", lastName: "Simpson").save()
        new Person(firstName: "Marge", lastName: "Simpson").save()
        new Person(firstName: "Bart", lastName: "Simpson").save()
        new Person(firstName: "Lisa", lastName: "Simpson").save()
        new Person(firstName: "Barney", lastName: "Rubble").save()
        new Person(firstName: "Fred", lastName: "Flinstone").save()
    }



    def getClassThatCallsWhere() {
        def gcl = new GroovyClassLoader(getClass().classLoader)
        gcl.parseClass('''
import grails.gorm.tests.*
import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform

@ApplyDetachedCriteriaTransform
class CallMe {
    def complexQuery() {
        Person.where {
            (lastName != "Simpson" && firstName != "Fred") || firstName == "Bart"
        }
    }

    def firstNameBartAndLastNameSimpson() {
        Person.where {
            firstName == "Bart" && lastName == "Simpson"
        }
    }
}
''', "Test").newInstance()
    }
}
