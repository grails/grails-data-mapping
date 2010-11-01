import com.basho.riak.client.RiakConfig
import org.junit.Test
import org.springframework.datastore.mapping.riak.util.RiakJavaClientTemplate
import org.springframework.datastore.mapping.riak.util.RiakTemplate

class RiakTemplateTests extends GroovyTestCase {

  @Test
  void testRiakTemplate() {
    RiakConfig cfg = new RiakConfig("localhost", "8098", "/riak")
    RiakTemplate tmpl = new RiakJavaClientTemplate(cfg)

    def obj1 = ["first_key": "first_value", "second_key": 1, "third_key": 1.2]

    // Store object
    tmpl.store("test", "test1", obj1)
    println "stored obj1: ${obj1}"

    // Read stored object
    def obj2 = tmpl.fetch("test", "test1")
    println "retrieved obj2: ${obj2}"
    assert obj2.first_key == "first_value"

    def children = tmpl.findByOwner("org.springframework.datastore.mapping.riak.TestEntity", "org.springframework.datastore.mapping.riak.TestEntity", "thisisatest", "children")
    println "children: ${children}"

    // Delete object
    tmpl.delete("test", "test1")
    def obj3 = tmpl.fetch("test", "test1")
    println "should be null: ${obj3}"
    assert obj3 == null

    // Store new object without an ID
    def obj4id = tmpl.store("test", obj1)
    println "stored obj1 with key: ${obj4id}"
    def obj5 = tmpl.fetch("test", obj4id)
    println "retrieved obj5: ${obj5}"
    assert obj5.first_key == "first_value"
    tmpl.delete("test", obj4id)
  }

}
