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
@Issue('https://github.com/grails/grails-core/issues/630')
class CountByInListWithEmptyListSpec extends GormSpec{

    void "Test countBy*InList with empty list" () {
        expect:
            Child.countByNameInList([]) == 0
    }

    @Override
    List getDomainClasses() {
        [Child]
    }
}
