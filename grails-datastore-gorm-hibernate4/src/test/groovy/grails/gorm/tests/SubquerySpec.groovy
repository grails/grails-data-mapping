package grails.gorm.tests

import grails.gorm.DetachedCriteria
import grails.persistence.Entity
import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform

/**
 * Tests for using subqueries in criteria and where method calls
 *
 */
@ApplyDetachedCriteriaTransform
class SubquerySpec extends GormDatastoreSpec {

    void "Test use alias in order by clause"() {
        given:"Some test data"
        def r1 = new Region(continent: "EMEA").save()
        def r2 = new Region(continent: "APAC").save()
        def e1 = new Employee(name:"Bob", region: r1).save()
        def e2 = new Employee(name:"Fred", region: r2).save()
        def e3 = new Employee(name:"Joe", region: r2).save()
        new Sale(employee: e1, total: 50000).save()
        new Sale(employee: e1, total: 150000).save()
        new Sale(employee: e2, total: 70000).save()
        session.clear()

        when:"A correlated subquery references the root query"
        def saleCriteria = new DetachedCriteria(Sale).build {
            employee {
                region('r') {
                    eq 'continent', 'EMEA'
                }
            }

        }.sort('r.continent', 'asc')
        def results = saleCriteria.list()

        then:"The results are correct"
        results.size() == 2
        results[0].employee.region.continent == 'EMEA'
        results[1].employee.region.continent == 'EMEA'
    }

    void "Test use alias in group by clause"() {
        given:"Some test data"
        def r1 = new Region(continent: "EMEA").save()
        def r2 = new Region(continent: "APAC").save()
        def e1 = new Employee(name:"Bob", region: r1).save()
        def e2 = new Employee(name:"Fred", region: r2).save()
        def e3 = new Employee(name:"Joe", region: r2).save()
        new Sale(employee: e1, total: 50000).save()
        new Sale(employee: e1, total: 150000).save()
        new Sale(employee: e2, total: 70000).save()
        session.clear()

        when:"A correlated subquery references the root query"
        def saleCriteria = new DetachedCriteria(Sale).build {
            employee {
                region('r')
            }

        }.projections {
            groupProperty('r.continent')
            rowCount()
        }
        def results = saleCriteria.list()

        then:"The results are correct"
        results.size() == 2
        results == [['APAC', 1], ['EMEA', 2]]
    }
    def "Test subquery with projection and criteria with closure"() {
        given:"A bunch of people"
        createPeople()

        when:"We query for people above a certain age average"
        def results = Person.withCriteria {
            gt "age",  {
                projections {
                    avg "age"
                }
            }

            order "firstName"
        }

        then:"the correct results are returned"
        results.size() == 4
        results[0].firstName == "Barney"
        results[1].firstName == "Fred"
        results[2].firstName == "Homer"
        results[3].firstName == "Marge"
    }

    def "Test subquery with projection and criteria"() {
        given:"A bunch of people"
        createPeople()

        when:"We query for people above a certain age average"
        def results = Person.withCriteria {
            gt "age", new DetachedCriteria(Person).build {
                projections {
                    avg "age"
                }
            }

            order "firstName"
        }

        then:"the correct results are returned"
        results.size() == 4
        results[0].firstName == "Barney"
        results[1].firstName == "Fred"
        results[2].firstName == "Homer"
        results[3].firstName == "Marge"
    }

    def "Test subquery that uses gtSome"() {
        given:"A bunch of people"
            createPeople()

        when:"We query for people above a certain age average"
            def results = Person.withCriteria {
                gtSome "age", Person.where { age < 18 }.age
                order "firstName"
            }

        then:"the correct results are returned"
            results.size() == 1
            results[0].firstName == "Bart"
    }

    def "Test subquery that uses in"() {
        given:"A bunch of people"
            createPeople()

        when:"We query for people above a certain age average"

            def results = Person.where {
                firstName in where { age < 18 }.firstName
            }.order('firstName').list()


        then:"the correct results are returned"
            results.size() == 2
            results[0].firstName == "Bart"
            results[1].firstName == "Lisa"
    }

    def "Test subquery that uses not in"() {
        given:"A bunch of people"
        createPeople()

        when:"We query for people not in a list of values using a subquery"

            def results = Person.withCriteria {
                notIn "firstName", Person.where { age < 18 }.firstName
                order "firstName"
            }


        then:"the correct results are returned"
        results.size() == 4
    }

    def "Test subquery that exists query"() {
        given:"A bunch of people"
            createPeople()

        when:"We query that uses an exists subquery"

            def results = Person.withCriteria {
                exists Person.where { age < 18 }.firstName
                order "firstName"
            }


        then:"the correct results are returned"
            results.size() == 6
    }


    def "Test subquery inside another where method"() {
        given:"A bunch of people"
            createPeople()

        when:"We query for people above a certain age average"
            def results = Person.where {
                age > where { age > 18 }.avg('age')
            }
            .order('firstName')
            .list()

        then:"the correct results are returned"
            results.size() == 2
            results[0].firstName == "Fred"
            results[1].firstName == "Homer"
    }

    void "Test correlated subquery with detached criteria"() {
        given:"Some test data"
        def r1 = new Region(continent: "EMEA").save()
        def r2 = new Region(continent: "APAC").save()
        def e1 = new Employee(name:"Bob", region: r1).save()
        def e2 = new Employee(name:"Fred", region: r2).save()
        new Sale(employee: e1, total: 50000).save()
        new Sale(employee: e1, total: 150000).save()
        new Sale(employee: e2, total: 70000).save()
        session.clear()

        when:"A correlated subquery is executed"

            def employees = new DetachedCriteria(Employee).build {
                region {
                    inList 'continent', ['APAC', "EMEA"]
                }
            }.id()

            def results = new DetachedCriteria(Sale).build {
                inList 'employee', employees
                gt 'total', 100000
            }.employee.list()


        then:"The results are correct"
            results.size() == 1
            results[0].name == "Bob"

        when:"A correlated subquery is executed with immutable lists and GStrings"

            employees = new DetachedCriteria(Employee).build {
                region {
                    inList 'continent', ["${'APAC'}", "EMEA"].asImmutable()
                }
            }.id()

            results = new DetachedCriteria(Sale).build {
                inList 'employee', employees
                gt 'total', 100000
            }.employee.list()


        then:"The results are correct"
            results.size() == 1
            results[0].name == "Bob"
    }

    void "Test correlated subquery"() {
        given:"Some test data"
            def r1 = new Region(continent: "EMEA").save()
            def r2 = new Region(continent: "APAC").save()
            def e1 = new Employee(name:"Bob", region: r1).save()
            def e2 = new Employee(name:"Fred", region: r2).save()
            new Sale(employee: e1, total: 50000).save()
            new Sale(employee: e1, total: 150000).save()
            new Sale(employee: e2, total: 70000).save()
            session.clear()

        when:"A correlated subquery is executed"

            def employees = Employee.where {
                region.continent in ['APAC', "EMEA"]
            }.id()

            def results = Sale.where {
                employee in employees && total > 100000
            }.employee.list()


        then:"The results are correct"
            results.size() == 1
            results[0].name == "Bob"

    }

    void "Test correlated subquery with root is queried with detached criteria only"() {
        given:"Some test data"
            def r1 = new Region(continent: "EMEA").save()
            def r2 = new Region(continent: "APAC").save()
            def e1 = new Employee(name:"Bob", region: r1).save()
            def e2 = new Employee(name:"Fred", region: r2).save()
            def e3 = new Employee(name:"Joe", region: r2).save()
            new Sale(employee: e1, total: 50000).save()
            new Sale(employee: e1, total: 150000).save()
            new Sale(employee: e2, total: 70000).save()
            session.clear()

        when:"A correlated subquery references the root query"
            def employeeCriteria = new DetachedCriteria(Employee, "e1")
            employeeCriteria.exists new DetachedCriteria(Sale, "s1").build {
                employee('e2') {
                    eqProperty "id", "e1.id"
                }
            }.id()
            def results = employeeCriteria.list()

        then:"The results are correct"
            results.size() == 2
    }

    void "Test correlated subquery with root is queried with detached criteria using aliases"() {
        given:"Some test data"
            def r1 = new Region(continent: "EMEA").save()
            def r2 = new Region(continent: "APAC").save()
            def e1 = new Employee(name:"Bob", region: r1).save()
            def e2 = new Employee(name:"Fred", region: r2).save()
            def e3 = new Employee(name:"Joe", region: r2).save()
            new Sale(employee: e1, total: 50000).save()
            new Sale(employee: e1, total: 150000).save()
            new Sale(employee: e2, total: 70000).save()
            session.clear()

        when:"A correlated subquery references the root query"
            def employeeCriteria = new DetachedCriteria(Employee).build {
                alias = 'e1'
            }
            employeeCriteria.exists new DetachedCriteria(Sale).build {
                alias = 's1'
                createAlias('employee', 'e2')
                employee {
                    eqProperty "id", "e1.id"
                }
            }.id()
            def results = employeeCriteria.list()

        then:"The results are correct"
            results.size() == 2
    }

    void "Test correlated subquery with root is queried using where query"() {
        given:"Some test data"
            def r1 = new Region(continent: "EMEA").save()
            def r2 = new Region(continent: "APAC").save()
            def e1 = new Employee(name:"Bob", region: r1).save()
            def e2 = new Employee(name:"Fred", region: r2).save()
            def e3 = new Employee(name:"Joe", region: r2).save()
            new Sale(employee: e1, total: 50000).save()
            new Sale(employee: e1, total: 150000).save()
            new Sale(employee: e2, total: 70000).save()
            session.clear()

        when:"A correlated subquery references the root query"
            def query = Employee.where {
                def em1 = Employee
                exists Sale.where {
                    def s1 = Sale
                    def em2 = employee
                    return em2.id == em1.id
                }.id()
            }
            def results = query.list()

        then:"The results are correct"
            results.size() == 2
    }

    protected def createPeople() {
        new Person(firstName: "Homer", lastName: "Simpson", age:45).save()
        new Person(firstName: "Marge", lastName: "Simpson", age:40).save()
        new Person(firstName: "Bart", lastName: "Simpson", age:9).save()
        new Person(firstName: "Lisa", lastName: "Simpson", age:7).save()
        new Person(firstName: "Barney", lastName: "Rubble", age:35).save()
        new Person(firstName: "Fred", lastName: "Flinstone", age:41).save()
    }

    @Override
    List getDomainClasses() {
        [Employee, Sale, Region]
    }

}

@Entity
class Employee {
    Long id
    Long version
    String name
    Region region
}
@Entity
class Sale {
    Long id
    Long version

    int total
    Employee employee
    static belongsTo = [employee: Employee]
}
@Entity
class Region {
    Long id
    Long version

    String continent
}


// Use to test where query transform:
//
//def results = new GroovyShell().evaluate('''
//import grails.gorm.tests.*
//import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform
//
//@ApplyDetachedCriteriaTransform
//class MyQuery {
//    static execute() {
//        Person.where {
//            firstName in where { age < 18 }.property('firstName')
//        }.list()
//    }
//}
//MyQuery.execute()
//''')