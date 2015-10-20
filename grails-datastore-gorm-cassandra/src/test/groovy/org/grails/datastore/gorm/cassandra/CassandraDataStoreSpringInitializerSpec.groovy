package org.grails.datastore.gorm.cassandra

import grails.cassandra.bootstrap.CassandraDatastoreSpringInitializer
import grails.gorm.tests.Artist
import org.springframework.context.ApplicationContext
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
class CassandraDataStoreSpringInitializerSpec extends Specification {

    void "Test the cassandra initializer works"() {
        when:"the initializer is run"
        def init = new CassandraDatastoreSpringInitializer([grails:[cassandra:[dbCreate:"recreate-drop-unused",keyspace:[name:'testspace', action:'create']]]],Cow)
        def ctx = init.configure()


        then:"GORM for Cassandra is loaded correct"
        Cow.count() == 0

        cleanup:
        ctx?.close()

    }
}


