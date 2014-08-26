package grails.gorm.tests

import com.datastax.driver.core.exceptions.InvalidQueryException

/**
 * @author graemerocher
 */
class ListOrderBySpec extends GormDatastoreSpec {

    void "Test listOrderBy property name method throws InvalidQueryException"() {
        given:
            def child = new ChildEntity(name: "Child")
            new TestEntity(age:30, name:"Bob", child:child).save()
            new TestEntity(age:55, name:"Fred", child:child).save()
            new TestEntity(age:17, name:"Jack", child:child).save()

        when:
            def results = TestEntity.listOrderByAge()

        then:
            //order by only supported when the partition key restricted by an EQ or IN which is not possible with listOrderBy
            thrown InvalidQueryException
    }
}
