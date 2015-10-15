package grails.gorm.tests

import grails.persistence.Entity
import org.grails.orm.hibernate.GormSpec
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
class CountByWithSortMappingSpec extends GormSpec {

    @Issue('https://github.com/grails/grails-core/issues/9298')
    void "Test count members"() {
        expect:
        Member.countByEmailIsNotNull() == 0
    }

    @Override
    List getDomainClasses() {
        [Member, CorporateMember, IndividualPerson]
    }
}

@Entity
abstract class Member {
    Long id
    Long version
    String name
    String email

    static mapping = {
        tablePerHierarchy false
        sort name: 'asc'   // <-- this is used in the count
    }
}

class CorporateMember extends Member {

}


class IndividualPerson extends Member {

}