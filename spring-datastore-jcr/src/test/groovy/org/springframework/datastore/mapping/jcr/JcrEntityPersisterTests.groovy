package org.springframework.datastore.mapping.jcr

import org.junit.Test
import org.junit.Ignore

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
class JcrEntityPersisterTests extends AbstractJcrTest {

  @Ignore
  @Test
  void testConnection() {
    assert null != conn;
    assert true == conn.isConnected();

    def session = conn.getNativeInterface();
    assert null != session;

  }

  @Test
  void testPersist() {
    ds.mappingContext.addPersistentEntity(TestEntity)
    def t = new TestEntity(title: "foo", body: "bar")
    conn.persist(t);
    assert null != t.id;

    t = conn.retrieve(TestEntity, t.id)
    assert t != null
    assert "foo" == t.title
    assert "bar" == t.body
    assert null != t.id

    t.title = 'blog'
    conn.persist(t)

    t = conn.retrieve(TestEntity, t.id)

    assert 'blog' == t.title
    assert 'bar' == t.body

    def id = t.id

    println id;

    conn.delete(t)
    conn.flush()

    t = conn.retrieve(TestEntity, id)
    assert t == null

  }

  @Ignore
  @Test
  void testNodeExists() {
    def session = conn.getNativeInterface()
    def root = session.getRootNode()
    printNode(root);
    if (session.itemExists("/TestEntity")) {
      def node = session.getRootNode().getNode("TestEntity")
      node.remove();
      session.save()
    }
    printNode(root)
  }

  /** Credit from http://jackrabbit.apache.org/first-hops.html     */
  /** Recursively outputs the contents of the given node.         */
  void printNode(def node) {
    // First output the node path
    System.out.println(node.getPath());
    // Skip the virtual (and large!) jcr:system subtree
    if (node.getName().equals("jcr:system")) {
      return;
    }

    // Then output the properties
    def properties = node.getProperties();
    while (properties.hasNext()) {
      def property = properties.nextProperty();
      if (property.getDefinition().isMultiple()) {
        // A multi-valued property, print all values
        def values = property.getValues();
        for (int i = 0; i < values.length; i++) {
          println(property.getPath() + " = " + values[i].getString());
        }
      } else {
        // A single-valued property
        println(property.getPath() + " = " + property.getString());
      }
    }
    // Finally output all the child nodes recursively
    def nodes = node.getNodes();
    while (nodes.hasNext()) {
      printNode(nodes.nextNode());
    }
  }
}



class TestEntity {
  //using id field as based APIs required,
  //the JCR generated UUID will be assigned to the id property.
  String id

  String title
  String body
}
