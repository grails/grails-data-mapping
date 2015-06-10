package grails.gorm.tests

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
@Issue('https://github.com/grails/grails-core/issues/8980')
class CriteriaSortSpec extends GormSpec {

    void "Test order by name in descending order with criteria"() {
        given:
        new Plant(name:"abc").save()
        new Plant(name:"bcd").save()
        new Plant(name:"xyz").save()
        new Plant(name:"lmn").save()
        new Plant(name:"qrs").save()
        new Plant(name:"nop").save()
        new Plant(name:"jkl").save()


        when:
        def cri = Plant.createCriteria()

        def results = cri.list {
            order('name',"asc")
        }*.name

        then:
        results == ['abc', 'bcd', 'jkl', 'lmn', 'nop', 'qrs', 'xyz']

        when:
        def cri2 = Plant.createCriteria()

        def results2 = cri2.list {
            order('name',"desc")
        }*.name

        then:
        results2 == ['xyz', 'qrs', 'nop', 'lmn', 'jkl', 'bcd', 'abc']
    }

    @Override
    List getDomainClasses() {
        [Plant]
    }
}
