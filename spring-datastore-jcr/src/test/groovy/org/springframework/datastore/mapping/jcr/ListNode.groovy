package org.springframework.datastore.mapping.jcr

import javax.jcr.Value
import javax.jcr.Session

/**
 * Created by IntelliJ IDEA.
 * User: Erawat
 * Date: 05-Oct-2010
 * Time: 10:47:40
 * To change this template use File | Settings | File Templates.
 */
class ListNode extends GroovyTestCase{

  void testListNodes(){
    def ds = new JcrDatastore()
    def connectionDetails = [username:"username",
                              password:"password",
                              workspace:"default",
                              configuration:"classpath:repository.xml",
                              homeDir:"/temp/repo"];
    def conn = ds.connect(connectionDetails)
    Session session = conn.getNativeInterface();
    def node = session.getNodeByUUID("1ce6978e-635e-4e40-80f3-391221186f92");
    printNode(node);
  }
    /** Recursively outputs the contents of the given node.  */
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
        Value[] values = property.getValues();
        for (int i = 0; i < values.length; i++) {
          System.out.println(property.getPath() + " = " + values[i].getString());
        }
      } else {
        // A single-valued property
        System.out.println(property.getPath() + " = " + property.getString());
      }
    }
    // Finally output all the child nodes recursively
    def nodes = node.getNodes();
    while (nodes.hasNext()) {
      printNode(nodes.nextNode());
    }
  }
  
}
