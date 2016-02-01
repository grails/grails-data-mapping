package grails.gorm.tests

import groovy.transform.NotYetImplemented
import org.grails.orm.hibernate.GormSpec
import spock.lang.Issue

/**
 * Created by graemerocher on 01/02/16.
 */
@Issue('https://github.com/grails/grails-core/issues/9290')
class CascadeToBidirectionalAsssociationSpec extends GormSpec {

    @NotYetImplemented
    void "test cascades work correctly with a bidirectional association"() {
        when:
        Club c = new Club(name: "Padres").save()
        Team padres = new Team(
                name: "Padres 1",
                club: c
        )


        def p = new Player(
            name: "John",
            contract: new Contract(
                    salary: 40_000_000
            )
        )
        padres.addToPlayers(p)

        // Desired behavior: Team cascades saves down to Player, which
        // cascades its saves down to Contract
        padres.save(flush: true)

        then:
        padres.id
    }
    @Override
    List getDomainClasses() {
        [Club, Team, Player, Contract]
    }
}
