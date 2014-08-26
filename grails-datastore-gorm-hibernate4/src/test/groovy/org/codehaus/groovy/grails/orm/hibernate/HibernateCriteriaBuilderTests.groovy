package org.codehaus.groovy.grails.orm.hibernate

import grails.orm.HibernateCriteriaBuilder
import grails.persistence.Entity

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsDomainClass

import org.junit.Test
import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class HibernateCriteriaBuilderTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [CriteriaBuilderTestClass, CriteriaBuilderTestClass2, OneAuthorPublisher, OneBookAuthor, Book, CriteriaBuilderBox]
    }

    List retrieveListOfNames() { ['bart'] }

    @Test
    void testResultTransformerWithMapParamToList() {
        def domainClass = ga.getDomainClass(CriteriaBuilderTestClass.name).clazz

        def obj = domainClass.newInstance()
        obj.firstName = "Jeff"
        obj.lastName = "Brown"
        obj.age = 196
        obj.addToChildren2(firstName:"Zack")
           .addToChildren2(firstName:"Jake")

        assertNotNull obj.save(flush:true)

        def results = domainClass.createCriteria().list {
            children2 { like 'firstName', '%a%' }
            setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY)
        }

        assertEquals 1, results.size()

        // per GRAILS-5692, the result transformer doesn't
        // work if a map is passed to the list method
        results = domainClass.createCriteria().list([:]) {
            children2 { like 'firstName', '%a%' }
            setResultTransformer(org.hibernate.Criteria.DISTINCT_ROOT_ENTITY)
        }

        assertEquals 1, results.size()
    }

    @Test
    void testSizeErrorMessages() {
        // GRAILS-6691
        def domainClass = ga.getDomainClass(CriteriaBuilderTestClass.name).clazz
        def crit = domainClass.createCriteria()

        def methodNames = ['sizeGt', 'sizeGe', 'sizeLe', 'sizeLt', 'sizeNe']
        methodNames.each { methodName ->
            def errorMessage = shouldFail(IllegalArgumentException) {
                crit."$methodName"('somePropertyName', 0)
            }
            assertEquals "Call to [${methodName}] with propertyName [somePropertyName] and size [0] not allowed here.", errorMessage
        }
    }

    @Test
    void testSqlProjection() {
        def domainClass = ga.getDomainClass(CriteriaBuilderBox.name).clazz

        assertNotNull(domainClass)

        def obj = domainClass.newInstance()
        obj.setProperty("width", 2)
        obj.setProperty("height", 7)
        obj.save()

        obj = domainClass.newInstance()
        obj.setProperty("width", 2)
        obj.setProperty("height", 8)
        obj.save()

        obj = domainClass.newInstance()
        obj.setProperty("width", 2)
        obj.setProperty("height", 9)
        obj.save()

        obj = domainClass.newInstance()
        obj.setProperty("width", 4)
        obj.setProperty("height", 9)
        obj.save()

        def results = CriteriaBuilderBox.withCriteria {
            projections {
                sqlProjection 'sum(width * height) as totalArea', 'totalArea', BYTE
            }
        }

        assert 1 == results?.size()
        assert 84 == results[0]
        assert results[0] instanceof Byte

        results = CriteriaBuilderBox.withCriteria {
            projections {
                sqlProjection 'sum(width * height) as totalArea', 'totalArea', SHORT
            }
        }

        assert 1 == results?.size()
        assert 84 == results[0]
        assert results[0] instanceof Short

        results = CriteriaBuilderBox.withCriteria {
            projections {
                sqlProjection 'sum(width * height) as totalArea', 'totalArea', INTEGER
            }
        }

        assert 1 == results?.size()
        assert 84 == results[0]
        assert results[0] instanceof Integer

        results = CriteriaBuilderBox.withCriteria {
            projections {
                sqlProjection 'sum(width * height) as totalArea', 'totalArea', LONG
            }
        }

        assert 1 == results?.size()
        assert 84 == results[0]
        assert results[0] instanceof Long

        results = CriteriaBuilderBox.withCriteria {
            projections {
                sqlProjection 'sum(width * height) as totalArea', 'totalArea', FLOAT
            }
        }

        assert 1 == results?.size()
        assert 84 == results[0]
        assert results[0] instanceof Float

        results = CriteriaBuilderBox.withCriteria {
            projections {
                sqlProjection 'sum(width * height) as totalArea', 'totalArea', DOUBLE
            }
        }

        assert 1 == results?.size()
        assert 84 == results[0]
        assert results[0] instanceof Double

        results = CriteriaBuilderBox.withCriteria {
            projections {
                sqlProjection 'sum(width * height) as totalArea', 'totalArea', BIG_INTEGER
            }
        }

        assert 1 == results?.size()
        assert 84 == results[0]
        assert results[0] instanceof BigInteger

        results = CriteriaBuilderBox.withCriteria {
            projections {
                sqlProjection 'sum(width * height) as totalArea', 'totalArea', BIG_DECIMAL
            }
        }

        assert 1 == results?.size()
        assert 84 == results[0]
        assert results[0] instanceof BigDecimal

        results = CriteriaBuilderBox.withCriteria {
            projections {
                sqlProjection '(2 * (width + height)) as perimeter, (width * height) as area', ['perimeter', 'area'], [INTEGER, INTEGER]
            }
        }

        assert 4 == results?.size()
        assert [18, 14] == results[0]
        assert [20, 16] == results[1]
        assert [22, 18] == results[2]
        assert [26, 36] == results[3]

        results = CriteriaBuilderBox.withCriteria {
            projections {
                sqlGroupProjection 'width, sum(height) as combinedHeightsForThisWidth', 'width', ['width', 'combinedHeightsForThisWidth'], [INTEGER, INTEGER]
            }
        }

        assert 2 == results?.size()
        assert [2, 24] == results[0]
        assert [4, 9] == results[1]
    }

    @Test
    void testSqlRestriction() {
        createDomainData()

        def domainClass = ga.getDomainClass(CriteriaBuilderTestClass.name).clazz

        // should retrieve bart and lisa, not homer and maggie
        def results = domainClass.withCriteria {
            sqlRestriction "char_length(first_name) <= 4"
        }

        assertEquals 2, results?.size()

        // should retrieve bart, lisa, homer and maggie
        results = domainClass.withCriteria {
            sqlRestriction "char_length(first_name) > 2"
        }

        // should retrieve homer, not bart, lisa and maggie
        results = domainClass.withCriteria {
            sqlRestriction 'char_length(first_name) > ? AND char_length(first_name) < ?', [4, 6]
        }

        // should retrieve homer, bart and lisa, not maggie
        results = domainClass.withCriteria {
            sqlRestriction 'char_length(first_name) > ? AND char_length(first_name) < ?', [2, 6]
        }

        assertEquals 3, results?.size()
    }

    @Test
    void testOrderByProjection() {
        createDomainData()

        def domainClass = ga.getDomainClass(CriteriaBuilderTestClass.name).clazz

        def results = domainClass.withCriteria {
            projections {
                property 'firstName', 'name'
            }
            order 'name', 'desc'
        }

        assertEquals "maggie", results[0]
        assertEquals "lisa", results[1]
        assertEquals "homer", results[2]
        assertEquals "bart", results[3]
    }

    // test for GRAILS-4377
    @Test
    void testResolveOrder() {
        def domainClass = ga.getDomainClass(CriteriaBuilderTestClass.name).clazz
        assertNotNull domainClass

        def obj = domainClass.newInstance()
        obj.firstName = "bart"
        obj.lastName = "simpson"
        obj.age = 11
        assertNotNull obj.save(flush:true)

        def action = {
            domainClass.createCriteria().list {
                'in'('firstName',retrieveListOfNames())
            }
        }

        def results = action()
        assertEquals 1 , results.size()
    }

    // test for GRAILS-3174
    @Test
    void testDuplicateAlias() {

        def domainClass = ga.getDomainClass(CriteriaBuilderTestClass.name).clazz

        def obj = domainClass.newInstance()
        obj.firstName = "Mike"
        obj.lastName="Simpson"
        obj.age=11
        obj.addToChildren2(firstName:"Groovy Joe")
           .addToChildren2(firstName:"Ted Grails")
           .addToChildren2(firstName:"ginger")

         assertNotNull obj.save(flush:true)

         def c = domainClass.createCriteria()
         def results =  c.listDistinct {
            and {
                'in'('firstName', ['Mike', 'Bob', 'Joe'])
                children2{
                    or{
                        like('firstName','%Groovy%')
                        like('firstName','%Grails%')
                    }
                }
                children2 {
                    ge('dateCreated',new Date() - 120)
                }
            }
        }

        assertEquals 1, results.size()
    }

    // test for GRAILS-7324
    @Test
    void testDuplicateAliasInPaginationParams() {

        def domainClass = ga.getDomainClass(CriteriaBuilderTestClass.name).clazz

        def obj = domainClass.newInstance()
        obj.firstName = "Bart"
        obj.lastName = "Simpson"
        obj.age = 11

        def obj2 = domainClass.newInstance()
        obj2.firstName = "Homer"
        obj2.lastName = "Simpson"
        obj2.age = 40
        obj2.addToChildren(obj)
        obj.parent = obj2

        assertNotNull obj2.save(flush: true)

        HibernateCriteriaBuilder hcb = new HibernateCriteriaBuilder(CriteriaBuilderTestClass, sessionFactory)
        hcb.grailsApplication = ga
        hcb.conversionService = new org.grails.datastore.mapping.model.types.conversion.DefaultConversionService()
        List results = hcb.list(sort: 'parent.firstName', order: 'asc') {
                    eq('firstName','Bart')
                    parent {
                      eq('lastName','Simpson')
                    }
                }
        assertEquals 1, results.size()
    }

    @Test
    void testWithGString() {
        def domainClass = ga.getDomainClass(CriteriaBuilderTestClass.name).clazz

        assertNotNull(domainClass)

        def obj = domainClass.newInstance()
        obj.firstName = "bart"
        obj.lastName="simpson"
        obj.age=11

        assertNotNull obj.save(flush:true)

        List results = domainClass.withCriteria {
            like('firstName',"${'ba'}%")
        }

        assertEquals 1 , results.size()
    }

    @Test
    void testAssociations() {
        def domainClass = ga.getDomainClass(CriteriaBuilderTestClass.name).clazz

        assertNotNull(domainClass)

        def obj = domainClass.newInstance()
        obj.setProperty("firstName", "homer")
        obj.setProperty("lastName", "simpson")
        obj.setProperty("age", 45)
        obj.save()

        def obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "bart")
        obj2.setProperty("lastName", "simpson")
        obj2.setProperty("age", 11)
        obj2.setProperty("parent", obj)
        obj2.save()

        def obj3 = domainClass.newInstance()
        obj3.setProperty("firstName", "list")
        obj3.setProperty("lastName", "simpson")
        obj3.setProperty("age", 9)
        obj3.setProperty("parent", obj)
        obj3.save()

        List results = domainClass.createCriteria().list {
            children {
                eq('firstName','bart')
            }
        }
        assertEquals 1 , results.size()
    }

    @Test
    void testNestedAssociations() {
        createDomainData()

        def domainClass = ga.getDomainClass(CriteriaBuilderTestClass.name).clazz
        List results = domainClass.createCriteria().list {
            children {
                eq('firstName','bart')
                children {
                    eq('firstName','lisa')
                }
            }
        }
        assertEquals 1 , results.size()
    }

    @Test
    void testInList() {
        createDomainData()
        def domainClass = ga.getDomainClass(CriteriaBuilderTestClass.name).clazz
        List results = domainClass.createCriteria().list {
            inList 'firstName', ["${'homer'}", 'bart'].asImmutable()
        }
        assertEquals 2, results.size()
        def firstNames = results*.firstName
        assertTrue 'homer' in firstNames
        assertTrue 'bart' in firstNames

        results = domainClass.createCriteria().list {
            or {
                inList 'firstName', []
                eq 'lastName', 'simpson'
            }
        }
        assertEquals 4, results.size()
        firstNames = results*.firstName
        assertTrue 'homer' in firstNames
        assertTrue 'bart' in firstNames
        assertTrue 'lisa' in firstNames
        assertTrue 'maggie' in firstNames

        results = domainClass.createCriteria().list {
            or {
                inList 'firstName', []
                inList 'lastName', ['simpson', 'adams']
            }
        }
        assertEquals 4, results.size()
        firstNames = results*.firstName
        assertTrue 'homer' in firstNames
        assertTrue 'bart' in firstNames
        assertTrue 'lisa' in firstNames
        assertTrue 'maggie' in firstNames

        results = domainClass.createCriteria().list {
            or {
                inList 'firstName', []
            }
        }
        assertEquals 0, results.size()

        results = domainClass.createCriteria().list {
            inList 'firstName', []
        }
        assertEquals 0, results.size()

        results = domainClass.createCriteria().list {
            inList 'firstName', []
            inList 'lastName', []
        }
        assertEquals 0, results.size()

        results = domainClass.createCriteria().list {
            or {
                inList 'firstName', []
                inList 'lastName', []
            }
        }
        assertEquals 0, results.size()

        def msg = shouldFail(GroovyRuntimeException) {
            results = domainClass.createCriteria().list {
                or {
                    inList 'firstName', null
                    eq 'lastName', 'simpson'
                }
            }
        }
        assertTrue msg.startsWith('Ambiguous method overloading for method grails.orm.HibernateCriteriaBuilder#inList')
    }

    private createDomainData() {
        def domainClass = ga.getDomainClass(CriteriaBuilderTestClass.name).clazz

        assertNotNull(domainClass)

        def obj = domainClass.newInstance()
        obj.setProperty("firstName", "homer")
        obj.setProperty("lastName", "simpson")
        obj.setProperty("age", 45)
        obj.save()

        def obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "bart")
        obj2.setProperty("lastName", "simpson")
        obj2.setProperty("age", 11)
        obj2.setProperty("parent", obj)
        obj2.save()

        def obj3 = domainClass.newInstance()
        obj3.setProperty("firstName", "lisa")
        obj3.setProperty("lastName", "simpson")
        obj3.setProperty("age", 9)
        obj3.setProperty("parent", obj2)
        obj3.save()

        def obj4 = domainClass.newInstance()
        obj4.setProperty("firstName", "maggie")
        obj4.setProperty("lastName", "simpson")
        obj4.setProperty("age", 9)
        obj4.save()
    }

    // TODO: The remaining tests in this test suite were migrated from a Java class and hence don't use very idiomatic Groovy
    // TODO: Need to tidy them up into more elegant Groovy code at some point

    @Test
    void testUniqueResult() {
        String clazzName = CriteriaBuilderTestClass.name
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
                clazzName)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "homer")
        obj.setProperty("lastName", "simpson")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

    // check that calling uniqueResult version of constructor
    // returns a single object

        Object result = parse(".list { " +
                        "eq('firstName','homer');" +
                "}", "Test1", CriteriaBuilderTestClass.name,true)

        assertEquals clazzName , result.getClass().getName()

    // check that calling the non-uniqueResult version of constructor
    // returns a List
        List results = parse(".list { " +
                        "eq('firstName','homer');" +
                "}", "Test1",CriteriaBuilderTestClass.name, false)
        assertTrue List.isAssignableFrom(results.getClass())
    }

    @Test
    void testNestedAssociation() {
        GrailsDomainClass domainClass =  grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "homer")
        obj.setProperty("lastName", "simpson")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "bart")
        obj2.setProperty("lastName", "simpson")
        obj2.setProperty("age", 11)
        obj2.setProperty("parent", obj)
        obj2.invokeMethod("save", null)

        GroovyObject obj3 = domainClass.newInstance()
        obj3.setProperty("firstName", "lisa")
        obj3.setProperty("lastName", "simpson")
        obj3.setProperty("age", 9)
        obj3.setProperty("parent", obj)
        obj3.invokeMethod("save", null)

        // now within or block
        List results = parse(".list { " +
                    "and {" +
                        "eq('lastName','simpson');" +
                        "children { " +
                            "eq('firstName','bart');" +
                        "}" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 1 , results.size()

        results = parse(".list { " +
                    "or {" +
                        "eq('firstName','lisa');" +
                        "children { " +
                            "eq('firstName','bart');" +
                        "}" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 2 , results.size()
    }

    @Test
    void testNestedAssociationIsNullField() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "homer")
        obj.setProperty("lastName", "simpson")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "bart")
        obj2.setProperty("lastName", null)
        obj2.setProperty("age", 11)
        obj2.setProperty("parent", obj)
        obj2.invokeMethod("save", null)

        GroovyObject obj3 = domainClass.newInstance()
        obj3.setProperty("firstName", "lisa")
        obj3.setProperty("lastName", "simpson")
        obj3.setProperty("age", 9)
        obj3.setProperty("parent", obj)
        obj3.invokeMethod("save", null)

        // now within or block
        List results = parse(".list { " +
                    "and {" +
                        "eq('lastName','simpson');" +
                        "children { " +
                            "isNull('lastName');" +
                        "}" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 1 , results.size()

        results = parse(".list { " +
                    "or {" +
                       "eq('lastName','simpson');" +
                        "children { " +
                            "isNotNull('lastName');" +
                        "}" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 2 , results.size()
    }

    @Test
    void testResultTransformer() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,CriteriaBuilderTestClass.name)
        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "homer")
        obj.setProperty("lastName", "simpson")
        obj.setProperty("age", 45)
        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "bart")
        obj2.setProperty("lastName", "simpson")
        obj2.setProperty("age", 11)
        obj2.setProperty("parent", obj)
        obj2.invokeMethod("save", null)

        GroovyObject obj3 = domainClass.newInstance()
        obj3.setProperty("firstName", "lisa")
        obj3.setProperty("lastName", "simpson")
        obj3.setProperty("age", 9)
        obj3.setProperty("parent", obj)
        obj3.invokeMethod("save", null)

        List results = parse(
            ".list { \n" +
                "or { \n" +
                    "gt('age', 40) \n" +
                    "children { \n" +
                        "eq('lastName','simpson') \n" +
                    "} \n" +
                "} \n" +
                "resultTransformer(org.hibernate.criterion.CriteriaSpecification.DISTINCT_ROOT_ENTITY) \n" +
            "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 1 , results.size()
    }

    @Test
    void testJunctions() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")
        obj2.setProperty("age", 42)
        obj2.invokeMethod("save", null)

        GroovyObject obj3 = domainClass.newInstance()
        obj3.setProperty("firstName", "dino")
        obj3.setProperty("lastName", "dinosaur")
        obj3.setProperty("age", 12)
        obj3.invokeMethod("save", null)

        GroovyObject obj4 = domainClass.newInstance()
        obj4.setProperty("firstName", "barney")
        obj4.setProperty("lastName", "rubble")
        obj4.setProperty("age", 45)
        obj4.invokeMethod("save", null)

        List results = parse("{ " +
                    "or { " +
                        "eq('firstName','fred');" +
                        "eq('lastName', 'flintstone');" +
                        "eq('age', 12);" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 3, results.size()

        results = parse("{ " +
                    "or { " +
                        "eq('lastName', 'flintstone');" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 2, results.size()

        results = parse("{ " +
                    "and { " +
                        "eq('age', 45);" +
                        "or { " +
                            "eq('firstName','fred');" +
                            "eq('lastName', 'flintstone');" +
                        "}" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 1, results.size()
    }

    @Test
    void testDistinct() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")
        obj2.setProperty("age", 42)
        obj2.invokeMethod("save", null)

        GroovyObject obj3 = domainClass.newInstance()
        obj3.setProperty("firstName", "dino")
        obj3.setProperty("lastName", "dinosaur")
        obj3.setProperty("age", 12)
        obj3.invokeMethod("save", null)

        List results = parse("{ " +
                    "projections { " +
                        "distinct('lastName');" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 2, results.size()
        assertTrue results.contains("flintstone")
        assertTrue results.contains("dinosaur")

        results = parse("{ " +
                    "projections { " +
                        "distinct(['lastName','age']);" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 3, results.size()
    }

    @Test
    void testHibernateCriteriaBuilder() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")
        obj2.setProperty("age", 42)
        obj2.invokeMethod("save", null)

        GroovyObject obj3 = domainClass.newInstance()
        obj3.setProperty("firstName", "dino")
        obj3.setProperty("lastName", "dinosaur")
        obj3.setProperty("age", 12)
        obj3.invokeMethod("save", null)

        List results = parse("{ " +
                    "and { " +
                        "eq('firstName','fred');" +
                        "eq('lastName', 'flintstone');" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        results = parse("{\n" +
                        "and {\n" +
                            "eq(\"firstName\",\"Fred\");\n" +
                            "and {\n" +
                                "eq(\"age\", 42)\n" +
                                "eq(\"lastName\", \"flintstone\")\n" +
                             "}\n" +
                        "}\n" +
                    "}", "Test2",CriteriaBuilderTestClass.name)
        results = parse("{\n" +
                        "eq(\"firstName\",\"Fred\");\n" +
                        "order(\"firstName\")\n" +
                        "maxResults(10)\n" +
                    "}", "Test3",CriteriaBuilderTestClass.name)

        shouldFail(MissingMethodException) {
            // rubbish argument
            results = parse("{\n" +
                    "and {\n" +
                        "eq(\"firstName\",\"Fred\")\n" +
                        "not {\n" +
                            "eq(\"age\", 42)\n" +
                            "rubbish()\n" +
                         "}\n" +
                    "}\n" +
                "}", "Test5",CriteriaBuilderTestClass.name)
        }
    }

    @Test
    void testProjectionProperty() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        List results = parse("{ " +
                    "projections { " +
                        "property('lastName',)" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 1, results.size()
    }

    @Test
    void testProjectionAvg() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")
        obj2.setProperty("age", 35)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                    "projections { " +
                        "avg('age',)" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)

    Double result = (Double) results.get(0)
        assertEquals 40, result.longValue()
   }

    @Test
   void testProjectionCount() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")
        obj2.setProperty("age", 35)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                    "projections { " +
                        "count('firstName')" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        assertEquals 2, results.get(0)
   }

   @Test
   void testProjectionCountDistinct() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")
        obj2.setProperty("age", 35)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                    "projections { " +
                        "countDistinct('lastName')" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        assertEquals 1, results.get(0)
   }

   @Test
   void testProjectionMax() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")
        obj2.setProperty("age", 35)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                    "projections { " +
                        "max('age')" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        assertEquals 45, results.get(0)
   }

   @Test
   void testProjectionMin() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")
        obj2.setProperty("age", 35)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                    "projections { " +
                        "min('age')" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        assertEquals 35, results.get(0)
   }

   @Test
   void testProjectionRowCount() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")
        obj2.setProperty("age", 35)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                    "projections { " +
                        "rowCount()" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        assertEquals 2, results.get(0)
   }

   @Test
   void testProjectionSum() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")
        obj2.setProperty("age", 35)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                    "projections { " +
                        "sum('age')" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        assertEquals 80, results.get(0)
   }

   @Test
   void testOrderAsc() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")
        obj2.setProperty("age", 35)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                    "projections { " +
                        "property('firstName');" +
                        "order('firstName', 'asc');" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)
   }

    @Test
   void testOrderDesc() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")
        obj2.setProperty("age", 35)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                    "projections { " +
                        "property('firstName');" +
                        "order('firstName','desc');" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)
   }

    @Test
   void testEqProperty() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "fred")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "mike")
        obj2.setProperty("lastName", "mike")
        obj2.setProperty("age", 45)

        obj2.invokeMethod("save", null)

        GroovyObject obj3 = domainClass.newInstance()
        obj3.setProperty("firstName", "wilma")
        obj3.setProperty("lastName", "flintstone")
        obj3.setProperty("age", 35)

        obj3.invokeMethod("save", null)

        List results = parse("{ " +
                        "eqProperty('firstName','lastName')" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        assertEquals 2, results.size()
   }

    @Test
   void testGtProperty() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "fred")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "zulu")
        obj2.setProperty("lastName", "alpha")
        obj2.setProperty("age", 45)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                        "gtProperty('firstName','lastName')" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        assertEquals 1, results.size()
   }

    @Test
   void testGe() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "fred")
        obj.setProperty("age", 43)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        //obj.setProperty("id", 2L)
        obj2.setProperty("firstName", "zulu")
        obj2.setProperty("lastName", "alpha")
        obj2.setProperty("age", 45)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                        "ge('age',43)" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        assertEquals 2, results.size()
   }

    @Test
   void testLe() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "fred")
        obj.setProperty("age", 43)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "zulu")
        obj2.setProperty("lastName", "alpha")
        obj2.setProperty("age", 45)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                        "le('age',45)" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 2, results.size()
   }

    @Test
   void testLt() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "fred")
        obj.setProperty("age", 43)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "zulu")
        obj2.setProperty("lastName", "alpha")
        obj2.setProperty("age", 45)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                        "lt('age',44)" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 1, results.size()
   }

    @Test
   void testEq() {
        GrailsDomainClass domainClass =  grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flinstone")
        obj.setProperty("age", 43)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "zulu")
        obj2.setProperty("lastName", "alpha")
        obj2.setProperty("age", 45)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                        "eq('firstName','fred')" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        assertEquals 1, results.size()
   }

    @Test
    void testEqCaseInsensitive() {
      GrailsDomainClass domainClass =  grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
      CriteriaBuilderTestClass.name)

      assertNotNull(domainClass)

      GroovyObject obj = domainClass.newInstance()
      obj.setProperty("firstName", "fred")
      obj.setProperty("lastName", "flinstone")
      obj.setProperty("age", 43)

      obj.invokeMethod("save", null)

      GroovyObject obj2 = domainClass.newInstance()
      obj2.setProperty("firstName", "zulu")
      obj2.setProperty("lastName", "alpha")
      obj2.setProperty("age", 45)

      obj2.invokeMethod("save", null)

      List results = parse("{ " +
            "eq('firstName','Fred')" +
            "}", "Test1",CriteriaBuilderTestClass.name)
      assertEquals 'default not ignoring case', 0, results.size()

      results = parse("{ " +
            "eq 'firstName','Fred', ignoreCase: false" +
            "}", "Test1",CriteriaBuilderTestClass.name)
      assertEquals 'explicitly not ignoring case', 0, results.size()

      results = parse("{ " +
            "eq 'firstName', 'Fred', ignoreCase: true" +
            "}", "Test1",CriteriaBuilderTestClass.name)
      assertEquals 'ignoring case should match one', 1, results.size()

      results = parse("{ " +
              "eq('firstName', 'Fred', [ignoreCase: true])" +
              "}", "Test1",CriteriaBuilderTestClass.name)
      assertEquals 'ignoring case should match one', 1, results.size()

      results = parse("{ " +
            "eq 'firstName', 'Fred', dontKnowWhatToDoWithThis: 'foo'" +
            "}", "Test1",CriteriaBuilderTestClass.name)
      assertEquals 'an unknown parameter should be ignored', 0, results.size()
   }

    @Test
    void testNe() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flinstone")
        obj.setProperty("age", 43)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "zulu")
        obj2.setProperty("lastName", "alpha")
        obj2.setProperty("age", 45)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                        "ne('firstName','fred')" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        assertEquals 1, results.size()
   }

    @Test
   void testLtProperty() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "fred")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "alpha")
        obj2.setProperty("lastName", "zulu")
        obj2.setProperty("age", 45)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                        "ltProperty('firstName','lastName')" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        assertEquals 1, results.size()
   }

    @Test
   void testGeProperty() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "fred")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "zulu")
        obj2.setProperty("lastName", "alpha")
        obj2.setProperty("age", 45)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                        "geProperty('firstName','lastName')" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 2, results.size()
   }

    @Test
   void testLeProperty() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "fred")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "alpha")
        obj2.setProperty("lastName", "zulu")
        obj2.setProperty("age", 45)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                        "leProperty('firstName','lastName')" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 2, results.size()
   }

    @Test
   void testNeProperty() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "fred")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "alpha")
        obj2.setProperty("lastName", "zulu")
        obj2.setProperty("age", 45)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                        "neProperty('firstName','lastName')" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 1, results.size()
   }

    @Test
   void testBetween() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "fred")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "alpha")
        obj2.setProperty("lastName", "zulu")
        obj2.setProperty("age", 42)

        obj2.invokeMethod("save", null)

        GroovyObject obj3 = domainClass.newInstance()
        obj3.setProperty("firstName", "wilma")
        obj3.setProperty("lastName", "flintstone")
        obj3.setProperty("age", 35)

        obj3.invokeMethod("save", null)

        List results = parse("{ " +
                        "between('age',40, 46)" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 2, results.size()
   }

    @Test
   void testIlike() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "fred")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "Flintstone")
        obj2.setProperty("age", 35)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                        "ilike('lastName', 'flint%')" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 1, results.size()
   }

    @Test
   void testIn() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")
        obj2.setProperty("age", 35)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                        "'in'('firstName',['fred','donkey'])" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        assertEquals 1, results.size()
   }

    @Test
   void testAnd() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "wilma")
        obj2.setProperty("lastName", "flintstone")
        obj2.setProperty("age", 35)

        obj2.invokeMethod("save", null)

        List results = parse("{ " +
                        "not{" +
                        "eq('age', 35);" +
                        "eq('firstName', 'fred');" +
                        "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 0, results.size()

        results = parse("{ " +
                        "not{" +
                        "eq('age', 35);" +
                        "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 1, results.size()

        shouldFail(IllegalArgumentException) {
            results = parse("{ " +
                    "not{" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        }
    }

    @Test
    void testIsNullAndIsNotNull() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "fred")
        obj.setProperty("lastName", "flintstone")
        obj.setProperty("age", 45)
        obj.invokeMethod("save", null)

        obj = domainClass.newInstance()
        obj.setProperty("firstName", "wilma")
        obj.setProperty("lastName", "flintstone")
        obj.invokeMethod("save", null)

        obj = domainClass.newInstance()
        obj.setProperty("firstName", "jonh")
        obj.setProperty("lastName", "smith")
        obj.invokeMethod("save", null)

        List results = parse("{ " +
                "isNull('age')" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 2, results.size()
        results = parse("{ " +
                "isNotNull('age')" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 1, results.size()
    }

    @Test
    void testPaginationParams() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)
        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "homer")
        obj.setProperty("lastName", "simpson")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        GroovyObject obj2 = domainClass.newInstance()
        obj2.setProperty("firstName", "bart")
        obj2.setProperty("lastName", "simpson")
        obj2.setProperty("age", 11)
        obj2.setProperty("parent", obj)
        obj2.invokeMethod("save", null)

        GroovyObject obj3 = domainClass.newInstance()
        obj3.setProperty("firstName", "list")
        obj3.setProperty("lastName", "simpson")
        obj3.setProperty("age", 9)
        obj3.setProperty("parent", obj)
        obj3.invokeMethod("save", null)

        // Try sorting on one of the string fields.
        List results = parse(".list(offset: 10, maxSize: 20, sort: 'firstName', order: 'asc') { " +
                    "children { " +
                        "eq('firstName','bart')" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        assertEquals 0 , results.size()

        // Now try sorting on the integer field.
        results = parse(".list(offset: 0, maxSize: 10, sort: 'age', order: 'asc') { " +
                    "children { " +
                        "eq('firstName','bart')" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)
        assertEquals 1 , results.size()
    }

    @Test
    void testPaginationParamsWithProjection() {
        GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
            CriteriaBuilderTestClass.name)

        assertNotNull(domainClass)

        GroovyObject obj = domainClass.newInstance()
        obj.setProperty("firstName", "homer")
        obj.setProperty("lastName", "simpson")
        obj.setProperty("age", 45)

        obj.invokeMethod("save", null)

        List results = parse(".list([:]) { " +
                    "projections { " +
                        "property('firstName')" +
                    "}" +
                "}", "Test1",CriteriaBuilderTestClass.name)

        assertEquals 1 , results.size()
        assertTrue 'Result list should contain Strings', results[0] instanceof String
    }

    @Test
    void testCriteriaInvolvingNestedRelationshipsSomeOfWhichAreEmbedded() {
        def newBook = new Book(title: 'First Popular Book')
        newBook.popularity = new Popularity(liked: 42)
        assert newBook.save(flush: true)

        def newAuthor = new OneBookAuthor()
        newAuthor.book = newBook
        assert newAuthor.save(flush: true)

        def newPublisher = new OneAuthorPublisher(name: 'First Good Publisher')
        newPublisher.author = newAuthor
        assert newPublisher.save(flush: true)

        newBook = new Book(title: 'Second Popular Book')
        newBook.popularity = new Popularity(liked: 2112)
        assert newBook.save(flush: true)

        newAuthor = new OneBookAuthor()
        newAuthor.book = newBook
        assert newAuthor.save(flush: true)

        newPublisher = new OneAuthorPublisher(name: 'Second Good Publisher')
        newPublisher.author = newAuthor
        assert newPublisher.save(flush: true)

        newBook = new Book(title: 'First Unppular Book')
        newBook.popularity = new Popularity(liked: 0)
        assert newBook.save(flush: true)

        newAuthor = new OneBookAuthor()
        newAuthor.book = newBook
        assert newAuthor.save(flush: true)

        newPublisher = new OneAuthorPublisher(name: 'First Bad Publisher')
        newPublisher.author = newAuthor
        assert newPublisher.save(flush: true)

        def result = OneAuthorPublisher.withCriteria {
            author {
                book {
                    popularity {
                        gt 'liked', 0
                    }
                }
            }
        }

        assertEquals 2, result?.size()

        def names = result.name
        assert 'First Good Publisher' in names
        assert 'Second Good Publisher' in names
    }

    private Object parse(String groovy, String testClassName, String criteriaClassName, boolean uniqueResult) {

        GroovyClassLoader cl = grailsApplication.getClassLoader()
        String unique = uniqueResult ? ",true" : ""
        Class clazz = cl.parseClass("""
package test

import grails.orm.*
import org.hibernate.*
import org.springframework.core.convert.ConversionService

class $testClassName {

   SessionFactory sf
   Class tc
   ConversionService cs

   Closure test = {
      def hcb = new HibernateCriteriaBuilder(tc, sf$unique)
      hcb.conversionService = cs
      return hcb$groovy
   }
}""")
        GroovyObject go = clazz.newInstance()
        go.setProperty("sf", sessionFactory)
        go.setProperty("tc", grailsApplication.getDomainClass(criteriaClassName).clazz)
        go.setProperty("cs", grailsApplication.mainContext.hibernateDatastore.mappingContext.conversionService)

        Closure closure = go.getProperty("test")
        return closure.call()
    }

    private Object parse(String groovy,String testClassName, String criteriaClassName) {
        return parse(groovy,testClassName,criteriaClassName,false)
    }
}

@Entity
class CriteriaBuilderTestClass {
    Long id
    Long version

    String firstName
    String lastName
    Integer age
    CriteriaBuilderTestClass parent
    Set children, children2
    static hasMany = [children:CriteriaBuilderTestClass, children2:CriteriaBuilderTestClass2]

    static constraints = {
        firstName(size:4..15)
        age(nullable:true)
        parent(nullable:true)
    }
}

@Entity
class CriteriaBuilderTestClass2 {
    Long id
    Long version

   String firstName
   Date dateCreated
}

@Entity
class CriteriaBuilderBox {
    Long id
    Long version

    int width
    int height
}
