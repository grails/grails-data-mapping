package grails.gorm.tests

import grails.persistence.Entity
import org.grails.orm.hibernate.GormSpec
import org.hibernate.FetchMode
import org.hibernate.collection.internal.PersistentSet
import org.hibernate.criterion.DetachedCriteria
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
@Issue('https://github.com/grails/grails-core/issues/2764')
class EagerFetchWithCriteriaSpec extends GormSpec {

    void "Test join fetching using the raw hibernate API"() {
        when:"A query is executed using a join"
            MenuItem item = new MenuItem(name:"Foo")
            item.addToSubscriptions(name:"Bar")
            item.save(flush:true)
            session.clear()
            def c = DetachedCriteria.forClass(MenuItem)
            c.setFetchMode('subscriptions', FetchMode.JOIN)
            item = c.getExecutableCriteria(session.datastore.sessionFactory.currentSession).list().get(0)


        then:"The association is eagerly initialised"
            item != null
            !(item instanceof PersistentSet)
            item.subscriptions.size() == 1
    }

    void "Test join fetching using GORM detached criteria"() {
        when:"A query is executed using a join"
            MenuItem item = new MenuItem(name:"Foo")
            item.addToSubscriptions(name:"Bar")
            item.save(flush:true)
            session.clear()
            grails.gorm.DetachedCriteria criteria = MenuItem.where {
                name == "Foo"
            }.join("subscriptions")

            item = criteria.get()


        then:"The association is eagerly initialised"
            item != null
            !(item instanceof PersistentSet)
            item.subscriptions.size() == 1
    }

    @Override
    List getDomainClasses() {
        [MenuItem, Subscription]
    }
}

@Entity
class MenuItem {
    Long id
    Long version
    String name
    Set subscriptions
    static hasMany = [subscriptions: Subscription]
}
@Entity
class Subscription {
    Long id
    Long version

    String name
}
