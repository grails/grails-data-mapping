package grails.gorm.tests

import grails.gorm.DetachedCriteria
import grails.persistence.Entity
import org.hibernate.QueryException
import spock.lang.Issue

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
class DetachedCriteriaNestedAssociationsSpec extends GormDatastoreSpec {

    @Issue('GRAILS-10879')
    void "Test that nested association queries with DetachedCriteria produce the correct result"() {

        given: "populate the levels"
        def l1 = new Level1(name: 'l1').save(failOnError: true)
        def l2 = new Level2(name: 'l2', level1: l1).save(failOnError: true)
        def l3 = new Level3(name: 'l3', level2: l2).save(failOnError: true)
        l1.addToLevel2s(l2).save(failOnError: true)
        l2.addToLevel3s(l3).save(failOnError: true)


        when: "create a normal detached criteria"
        def c = new DetachedCriteria(Level3).build {
            level2 {
                level1 {
                    eq('name', 'l1')
                }
            }
        }
        def result = c.list()

        then: "behave as expected"
        1 == result.size()
        'l3' == result[0].name



        when: "create a detached criteria with a junction and no nested filters"
        c = new DetachedCriteria(Level3).build {
            and {
                eq('name', 'l3')
            }
        }
        result = c.list()

        then: "behave as expected"
        1 == result.size()
        'l3' == result[0].name



        when: "create a detached criteria with a junction and 1 level "
        c = new DetachedCriteria(Level3).build {
            and {
                level2 {
                    eq('name', 'l2')
                }
            }
        }
        result = c.list()

        then: "behave as expected"
        1 == result.size()
        'l3' == result[0].name



        when: "create a detached criteria with a junction and 2 levels "
        c = new DetachedCriteria(Level3).build {
            and {
                level2 {
                    level1 {
                        eq('name', 'l1')
                    }
                }
            }
        }
        result = c.list()

        then: "behave as expected"
        result.size() == 1

    }

    @Override
    List getDomainClasses() {
        [Level1, Level2, Level3]
    }
}

@Entity
class Level1 {
    Long id
    Long version

    String name
    Set level2s
    static hasMany = [level2s : Level2]
    static constraints = {
    }
}

@Entity
class Level2 {
    Long id
    Long version

    String name
    Level1 level1
    Set level3s
    static belongsTo = [level1: Level1]
    static hasMany = [level3s : Level3]

    static constraints = {
    }
}

@Entity
class Level3 {
    Long id
    Long version
    String name
    Level2 level2
    static belongsTo = [level2: Level2]

    static constraints = {
    }
}
