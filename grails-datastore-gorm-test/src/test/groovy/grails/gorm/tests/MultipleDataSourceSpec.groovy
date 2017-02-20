package grails.gorm.tests

import grails.gorm.DetachedCriteria
import grails.gorm.annotation.Entity
import grails.gorm.transactions.Transactional
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by graemerocher on 10/01/2017.
 */
class MultipleDataSourceSpec extends Specification {

    @Shared @AutoCleanup SimpleMapDatastore datastore = new SimpleMapDatastore([ConnectionSource.DEFAULT, "one"], Player)

    void "test multiple datasource support with in-memory GORM"() {
        given:
        new Player(name: "Giggs").save(flush:true)
        PlayerService service = new PlayerService()

        expect:
        Player.count() == 1
        new DetachedCriteria<>(Player).count() == 1
        new DetachedCriteria<>(Player).withConnection("one").count() == 0
        Player.one.count() == 0
        service.countPlayers() == 1
        service.countPlayersOne() == 0
    }
}

@Entity
class Player {
    String name

    static mapping = {
        datasources ConnectionSource.DEFAULT, "one"
    }
}

@Transactional
class PlayerService {

    @Transactional(connection = "one")
    Number countPlayersOne() {
        // check the right datastore transaction is being used
        assert transactionStatus.transaction.sessionHolder.sessions.first().datastore.backingMap[Player.name].isEmpty()
        Player.one.count()
    }


    Number countPlayers() {
        assert !transactionStatus.transaction.sessionHolder.sessions.first().datastore.backingMap[Player.name].isEmpty()
        Player.count()
    }
}