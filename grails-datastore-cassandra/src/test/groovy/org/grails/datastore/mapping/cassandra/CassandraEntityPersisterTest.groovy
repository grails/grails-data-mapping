package org.grails.datastore.mapping.cassandra

import org.junit.Test
import org.grails.datastore.mapping.cassandra.uuid.UUIDUtil
import org.grails.datastore.mapping.core.Session
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class CassandraEntityPersisterTest extends AbstractCassandraTest {

	@Test
	void testReadWrite() {
		def ds = new CassandraDatastore()

		ds.mappingContext.addPersistentEntity(TestEntity)
		Session conn = ds.connect(null)
		conn.applicationEventPublisher = new ApplicationEventPublisher() {
			@Override
			void publishEvent(ApplicationEvent applicationEvent) {
				println applicationEvent
			}
		}


		def t = conn.retrieve(TestEntity, UUIDUtil.getRandomUUID())


		assert t == null

		t = new TestEntity(name: "Bob", age: 45)

		conn.persist(t)

		conn.flush()
		assert t.id != null

		def t2 = conn.retrieve(TestEntity, t.id)

		println t2.id.toString() + " - " + t2.name

		assert t2 != null
		assert "Bob" == t2.name
		assert 45 == t2.age
		assert t2.id != null


		t.age = 55
		conn.persist(t)

		t = conn.retrieve(TestEntity, t.id)

		assert 55 == t.age
	}
}

class TestEntity {
	UUID id
	String name
	int age
}
