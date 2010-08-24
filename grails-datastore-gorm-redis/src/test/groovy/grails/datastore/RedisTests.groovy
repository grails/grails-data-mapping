package grails.datastore

import org.junit.After
import org.junit.Before
import org.springframework.datastore.core.Session
import static org.grails.datastore.gorm.redis.RedisSetup.setup
import org.junit.Test
import grails.gorm.tests.TestEntity
import grails.gorm.tests.ChildEntity

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 23, 2010
 * Time: 12:46:51 PM
 * To change this template use File | Settings | File Templates.
 */
class RedisTests {

  Session session
  @Before
  void setupRedis() {
    session = setup()
  }

  @After
  void disconnect() {
    session.disconnect()
  }


  @Test
  void testRedisList() {
    def redis = new Redis(session.datastore, session.nativeInterface)

    redis.flushall()

    def list = redis.list("my.list")

    list << 1 << 2 << 3

    assert 3 == list.size()
    assert 1 == list[0] as Integer
    assert 2 == list[1] as Integer
    assert 3 == list[2] as Integer
  }

  @Test
  void testEntities() {

    def redis = new Redis(session.datastore, session.nativeInterface)

    def age = 40                                                                                                 

    ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each {
      new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
    }


    def results = TestEntity.list()

    assert 6 == results.size()

    def list = redis.list("my.list")
    results.each { list << it.id }

    def entities = redis.entities(TestEntity, "my.list")

    assert 6 == entities.size()

    assert entities.find { it.name == "Fred"}
  }
}
