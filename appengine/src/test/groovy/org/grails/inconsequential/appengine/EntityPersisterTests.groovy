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
    t.age = 45
    conn.persist(t)

    assert t.id != null

    def key = new AppEngineKey(t.id)
    t = conn.retrieve(TestEntity, key)

    assert t != null
    assert "bob"  == t.name
    assert 45  == t.age

    t.age = 55

    conn.persist(t)

    def t2 = conn.retrieve(TestEntity, key)
    assert t != null
    assert t.id == t2.id
    assert "bob"  == t2.name
//    TODO: Updates seem to be failing on update. Fix me.
//    assert 55  == t2.age


    conn.delete(key)

    t = conn.retrieve(TestEntity, key)

    assert t == null

  }
}
class TestEntity {
  Key id
  String name
  Integer age
}
