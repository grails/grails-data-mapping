package grails.gorm.tests

import grails.gorm.services.Service
import grails.gorm.tests.GormDatastoreSpec
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.gorm.events.ConfigurableApplicationEventPublisher
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEventListener
import org.grails.datastore.mapping.engine.event.PreInsertEvent
import org.grails.datastore.mapping.engine.event.PreUpdateEvent
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ConfigurableApplicationContext

class DirtyCheckingAfterListenerSpec extends GormDatastoreSpec {

    TestSaveOrUpdateEventListener listener

    @Override
    List getDomainClasses() {
        return [Player]
    }

    def setup() {
        listener = new TestSaveOrUpdateEventListener(session.datastore)
        ApplicationEventPublisher publisher = session.datastore.applicationEventPublisher
        if (publisher instanceof ConfigurableApplicationEventPublisher) {
            ((ConfigurableApplicationEventPublisher) publisher).addApplicationListener(listener)
        } else if (publisher instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext) publisher).addApplicationListener(listener)
        }
    }

    void "test state change from listener update the object"() {

        setup:
        PlayerService playerService = session.datastore.getService(PlayerService)
        Player john = playerService.save("John")

        when:
        session.flush()
        john = playerService.find("John")

        then:
        john.attributes
        john.attributes.size() == 3
    }

    static class TestSaveOrUpdateEventListener extends AbstractPersistenceEventListener {

        TestSaveOrUpdateEventListener(Datastore datastore) {
            super(datastore)
        }

        @Override
        protected void onPersistenceEvent(AbstractPersistenceEvent event) {
            Player player = (Player) event.entityObject
            player.attributes = ["test0", "test1", "test2"]
        }

        @Override
        boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
            return eventType == PreUpdateEvent || eventType == PreInsertEvent
        }
    }
}

class Player implements GormEntity<Player> {
    String name
    List<String> attributes

}

@Service(Player)
interface PlayerService {

    Player save(String name)

    Player find(String name)
}

