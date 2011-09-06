package grails.gorm.tests

import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 9/6/11
 * Time: 3:41 PM
 * To change this template use File | Settings | File Templates.
 */
class WhereMethodSpec extends GormDatastoreSpec {

    def "Test basic binary criterion where call"() {
        given:"A bunch of people"
            new Person(firstName:"Homer", lastName: "Simpson").save()
            new Person(firstName:"Marge", lastName: "Simpson").save()
            new Person(firstName:"Bart", lastName: "Simpson").save()
            new Person(firstName:"Lisa", lastName: "Simpson").save()
            new Person(firstName:"Barney", lastName: "Rubble").save()
            new Person(firstName:"Fred", lastName: "Flinstone").save()

        when: "A where query is used"
            def query = classThatCallsWhere.firstNameBartAndLastNameSimpson()
            def result = query.get()

        then:"The correct result is returned"

            result != null
            result.firstName == "Bart"

    }

    def "Test basic single criterion where call"() {
        given:"A bunch of people"
            new Person(firstName:"Homer", lastName: "Simpson").save()
            new Person(firstName:"Marge", lastName: "Simpson").save()
            new Person(firstName:"Bart", lastName: "Simpson").save()
            new Person(firstName:"Lisa", lastName: "Simpson").save()
            new Person(firstName:"Barney", lastName: "Rubble").save()
            new Person(firstName:"Fred", lastName: "Flinstone").save()

        when: "A where query is used"
            def query = classThatCallsWhere.firstNameBart()
            def result = query.get()

        then:"The correct result is returned"

            result != null
            result.firstName == "Bart"

    }



    def getClassThatCallsWhere() {
        def gcl = new GroovyClassLoader(getClass().classLoader)
        gcl.parseClass('''
import grails.gorm.tests.*
import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform

@ApplyDetachedCriteriaTransform
class CallMe {
    def firstNameBart() {
        Person.where {
            firstName == "Bart"
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
