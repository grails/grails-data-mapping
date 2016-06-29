package grails.gorm.tests

import org.grails.orm.hibernate.GormSpec

/**
 * Created by graemerocher on 31/01/16.
 */
class AutoAssociationInverseDetachedEntitySpec extends GormSpec {

    void "Test auto associate inverse detached entities"() {
        when:"An inverse entity is saved and detached"
        def c = new Club(name: "Manchester United").save(flush: true)
        c.discard()

        def team = new Team(name: "First Team", club: c)
        team.save(flush:true)

        then:"The save was successful"
        team.id != null

    }

    @Override
    List getDomainClasses() {
        [Team, Club]
    }
}
