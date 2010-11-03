package org.springframework.datastore.mapping.mongo


import org.junit.Test;
import org.springframework.datastore.mapping.core.Session;


class BasicPersistenceSpec {

	@Test
	void testBasicPersistenceOperations() {
      def md = new MongoDatastore()
      md.afterPropertiesSet()
      md.mappingContext.addPersistentEntity(TestEntity)




      MongoSession session = md.connect()

      session.nativeInterface.dropDatabase()

      def te = new TestEntity(name:"Bob")

      session.persist te
      session.flush()

      assert te != null
      assert te.id != null

      session.clear()
      te = session.retrieve(TestEntity, te.id)

      assert te != null
      assert te.name == "Bob"

      te.name = "Fred"
      session.persist(te)
      session.flush()
      session.clear()


      te = session.retrieve(TestEntity, te.id)
      assert te != null
      assert te.id != null
      assert te.name == 'Fred'

      session.delete te
      session.flush()


      te = session.retrieve(TestEntity, te.id)
      assert te == null

	}
}
class TestEntity {
	
    Long id
	String name
}
