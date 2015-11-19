package test




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
class HibernateGrails2PluginSpec extends spock.lang.Specification {

    void "Test that hibernate works with Grails 2"() {
        when:"A new book is created that is invalid"
        def b = new Book(title:"")

        then:"the book is invalid"
        !b.validate()
        !b.save()

        when:"the book is made valid"
        b.title = "The Stand"

        then:"It can be saved"
        b.save(flush:true)
        Book.count() == 1
    }
}

