package grails.datastore

import grails.gorm.tests.*

/**
 * @author graemerocher
 */
class RedisSpec extends GormDatastoreSpec {

    def "Test usage of subscript operator"() {
        given:
            def redis = new Redis(datastore:session.datastore)

        when:
            redis.flushall()
            redis["foo"] = "bar"

        then:
            "bar" == redis["foo"]
    }

    def testRedisList() {
        given:
            def redis = new Redis(datastore:session.datastore)

        when:
            redis.flushall()
            def list = redis.list("my.list")
            list << 1 << 2 << 3

        then:
            3 == list.size()
            1 == list[0] as Integer
            2 == list[1] as Integer
            3 == list[2] as Integer
    }

    def testEntities() {
        given:
            def redis = new Redis(datastore:session.datastore)
            def age = 40
            ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each {
                new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
            }
            def results = TestEntity.list()

        expect:
            6 == results.size()

        when:
            def list = redis.list("my.list")
            results.each { list << it.id }
            def entities = redis.entities(TestEntity, "my.list")

        then:
            6 == entities.size()
            entities.find { it.name == "Fred"}
    }
}
