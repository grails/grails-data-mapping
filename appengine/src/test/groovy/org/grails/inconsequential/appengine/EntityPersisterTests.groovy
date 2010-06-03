package org.grails.inconsequential.appengine

import com.google.appengine.api.datastore.Key
import org.grails.inconsequential.core.DatastoreContext
import org.grails.inconsequential.appengine.testsupport.AppEngineDatastoreTestCase
import org.grails.inconsequential.core.ObjectDatastoreConnection

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class EntityPersisterTests extends AppEngineDatastoreTestCase {



  void testPersistObject() {
    AppEngineDatastore ds = new AppEngineDatastore()

    ObjectDatastoreConnection conn = ds.connect(null)

    ds.getMappingContext().addPersistentEntity(TestEntity)

    TestEntity t = new TestEntity()
    t.name = "bob"
    conn.persist(t)

    assert t.id != null

    def key = new AppEngineKey(t.id)
    t = conn.retrieve(TestEntity, key)

    assert t != null
    assert "bob"  == t.name

    conn.delete(key)

    t = conn.retrieve(TestEntity, key)

    assert t == null

  }
}
class TestEntity {
  Key id
  String name
}
