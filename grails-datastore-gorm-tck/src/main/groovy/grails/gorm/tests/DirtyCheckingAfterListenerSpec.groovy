package grails.gorm.tests

import grails.gorm.annotation.Entity
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
import spock.util.concurrent.PollingConditions

class DirtyCheckingAfterListenerSpec extends GormDatastoreSpec {

    TestSaveOrUpdateEventListener listener
    def datastore

    @Override
    List getDomainClasses() {
        return [Player]
    }

    def setup() {
        datastore = session.datastore
        listener = new TestSaveOrUpdateEventListener(datastore)
        ApplicationEventPublisher publisher = datastore.applicationEventPublisher
        if (publisher instanceof ConfigurableApplicationEventPublisher) {
            ((ConfigurableApplicationEventPublisher) publisher).addApplicationListener(listener)
        } else if (publisher instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext) publisher).addApplicationListener(listener)
        }
    }

    void "test state change from listener update the object"() {

        when:
        new Player(name: "John").save()

        then:
        new PollingConditions().eventually { listener.isExecuted && Player.count()}

        when:
        session.flush()
        session.clear()
        Player john = Player.get(john.id)

        then:
        john.attributes
        john.attributes.size() == 3

    }
}

@Entity
class Player implements GormEntity<Player> {
    String name
    List<String> attributes

}

class TestSaveOrUpdateEventListener extends AbstractPersistenceEventListener {

    boolean isExecuted = false

    TestSaveOrUpdateEventListener(Datastore datastore) {
        super(datastore)
    }

    @Override
    protected void onPersistenceEvent(AbstractPersistenceEvent event) {
        Player player = (Player) event.entityObject
        player.attributes = ["test0", "test1", "test2"]
        isExecuted = true
    }

    @Override
    boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return eventType == PreUpdateEvent || eventType == PreInsertEvent
    }
}