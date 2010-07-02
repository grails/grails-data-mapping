package org.springframework.datastore.groovy

import org.springframework.datastore.core.Datastore
import org.springframework.datastore.core.Session
import org.springframework.datastore.mapping.PersistentEntity

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
                Session connection = datastore.currentSession
                connection.persist(delegate)
              }

              delete {->
                Session connection = datastore.currentSession
                connection.delete delegate
              }

              'static' {
                  get { Serializable id ->
                    Session connection = datastore.currentSession
                    connection.retrieve(javaClass, id)
                  }

                  deleteAll { Object[] objs ->
                    Session connection = datastore.currentSession
                    connection.delete objs
                  }
              }
          }

      }

    }
  }
}
