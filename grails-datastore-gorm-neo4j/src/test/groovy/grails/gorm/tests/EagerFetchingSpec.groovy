package grails.gorm.tests

import org.grails.datastore.gorm.neo4j.collection.Neo4jList
import org.grails.datastore.gorm.neo4j.collection.Neo4jPersistentList
import spock.lang.Specification

/*
 * Copyright 2014 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author graemerocher
 */
class EagerFetchingSpec extends GormDatastoreSpec {

    void "Test eager fetch with query"() {
        given:
        def club = new Club(name: 'FC Bayern Muenchen')
        club.addToTeams(new Team(name: 'FCB Team 1'))
        club.addToTeams(new Team(name: 'FCB Team 2'))
        club.save(flush:true)
        session.clear()

        when:"a lazy query is executed"
        club = Club.get(club.id)

        then:"a lazy collection is loaded"
        club.teams instanceof Neo4jPersistentList
        !club.teams.isInitialized()
        club.teams[0] instanceof Team
        club.teams[0].name == 'FCB Team 1'
        club.teams[1].name == 'FCB Team 2'

        when:"A join query is executed"
        session.clear()
        club = Club.findByName('FC Bayern Muenchen', [fetch:[teams:'eager']])

        then:"A join query was issued and so the collection is initialized"
        club.teams
        club.teams instanceof Neo4jList
        club.teams[0] instanceof Team
        club.teams[0].name == 'FCB Team 1'
        club.teams[1].name == 'FCB Team 2'

        when:"A lazy to one association is queried"
        session.clear()
        def team = Team.findByName('FCB Team 1')

        then:"The association is a proxy"
        session.mappingContext.proxyFactory.isProxy(team.club)

        when:"A an eager fetch is ued"
        session.clear()
         team = Team.findByName('FCB Team 1', [fetch:[club:'eager']])

        then:"The association is a proxy"
        !session.mappingContext.proxyFactory.isProxy(team.club)
    }

    @Override
    List getDomainClasses() {
        [Club, Team]
    }
}
