package org.grails.inconsequential.appengine

import com.google.appengine.api.datastore.Key
import org.grails.inconsequential.core.DatastoreContext

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class EntityPersisterTests extends AppEngineDatastoreTest {



  void testPersistObject() {
    AppEngineDatastore ds = new AppEngineDatastore()
    ds.getMappingContext().addPersistentEntity(TestEntity)

    def t = new TestEntity()
    def conn = ds.connect(null)
    ds.persist(new AppEngineContext(conn, ds.mappingContext), t)

    assert t.id != null

  }

  class TestEntity {
    Key id
    String name
  }
  
}
