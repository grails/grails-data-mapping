package grails.gorm.tests

import org.junit.Test

/**
 * Abstract base test for loading proxies. Subclasses should do the necessary setup to configure GORM
 */
abstract class ProxyLoadingTests {

  @Test
  void testProxy() {

    def t = new TestEntity(name:"Bob", age: 45, child:new ChildEntity(name:"Test Child")).save()

    def proxy = TestEntity.load(t.id)

    assert proxy
    assert t.id == proxy.id

    assert "Bob" == proxy.name
  }

  @Test
  void testProxyWithQueryByAssociation() {
    def child = new ChildEntity(name: "Test Child")
    def t = new TestEntity(name:"Bob", age: 45, child:child).save()


    def proxy = ChildEntity.load(child.id)

    t = TestEntity.findByChild(proxy)

    assert t

  }
}
