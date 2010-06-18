package org.grails.inconsequential.runtime

import org.grails.inconsequential.mapping.PersistentEntity
import org.grails.inconsequential.core.Datastore
import org.grails.inconsequential.core.ObjectDatastoreConnection

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class RuntimeCapabilities {
  Datastore datastore

  RuntimeCapabilities(Datastore datastore) {
    this.datastore = datastore;
  }

  void enhance() {
    def datastore = this.datastore
    def mappingContext = datastore.mappingContext
    if(mappingContext) {
      for(PersistentEntity e in mappingContext.persistentEntities) {
        def javaClass = e.javaClass

          javaClass.metaClass {
              save {->
                ObjectDatastoreConnection connection = datastore.currentConnection
                connection.persist(delegate)
              }

              delete {->
                ObjectDatastoreConnection connection = datastore.currentConnection
                connection.delete delegate
              }

              'static' {
                  get { Serializable id ->
                    ObjectDatastoreConnection connection = datastore.currentConnection
                    connection.retrieve(javaClass, connection.createKey(id))
                  }

                  deleteAll { Object[] objs ->
                    ObjectDatastoreConnection connection = datastore.currentConnection
                    connection.delete objs
                  }
              }
          }

      }

    }
  }
}
