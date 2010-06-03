package org.grails.inconsequential.runtime

import org.grails.inconsequential.core.ObjectDatastoreConnection

/**
 * @author Graeme Rocher
 * @since 1.0
 */

@Category(Object)
class SimplePersistenceMethods {
  ObjectDatastoreConnection datastore


  SimplePersistenceMethods(datastore) {
    this.datastore = datastore;
  }

  void save() {
    datastore.persist(null,this)  
  }
}
