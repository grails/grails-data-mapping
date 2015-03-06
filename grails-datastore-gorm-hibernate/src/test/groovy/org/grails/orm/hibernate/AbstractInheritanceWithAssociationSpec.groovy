package org.grails.orm.hibernate

import grails.persistence.Entity

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
class AbstractInheritanceWithAssociationSpec extends GormSpec{

    void "Test persist entity with abstract parent class"() {
        when:"An association is persisted that contains an abstract reference"
            Message m = new Message(member: new MemberTypeA(name:"foo",attributeA: "test").save())
            m.save(flush:true)
            println m.errors
            session.clear()
            m = Message.get(m.id)
        then:"The association is correct"
            m.member instanceof MemberTypeA
            m.member.attributeA == 'test'
    }
    @Override
    List getDomainClasses() {
        [Member, MemberTypeA, MemberTypeB, Message]
    }
}

@Entity
abstract class Member {
    String name
    static constraints = {
    }
}

@Entity
class MemberTypeA extends Member {
    String attributeA
    static constraints = {
    }
}

@Entity
class MemberTypeB extends Member {
    String attributeB
    static constraints = {
    }
}

@Entity
class Message {
    Member member
    static constraints = {
    }
}