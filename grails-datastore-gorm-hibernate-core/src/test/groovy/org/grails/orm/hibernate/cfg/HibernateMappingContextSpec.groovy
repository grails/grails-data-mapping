package org.grails.orm.hibernate.cfg

import grails.gorm.annotation.Entity
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ManyToMany
import org.grails.datastore.mapping.model.types.ManyToOne
import org.grails.datastore.mapping.model.types.OneToMany
import org.grails.datastore.mapping.model.types.OneToOne
import org.grails.datastore.mapping.model.types.ToOne
import spock.lang.IgnoreRest
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
class HibernateMappingContextSpec extends Specification {

    void "Test create a Hibernate mapping context"() {
        when:
        def mappingContext = new HibernateMappingContext({->
            autowire false
        })
        mappingContext.addPersistentEntities(PersonDSL)

        mappingContext.initialize()
        def entity = mappingContext.getPersistentEntity(PersonDSL.name)

        then:
        entity instanceof HibernatePersistentEntity
        entity.mapping.mappedForm instanceof Mapping
        entity.mapping.mappedForm.autowire == false
        entity.mapping.mappedForm.table.name == 'people'
        entity.mapping.identifier.mappedForm instanceof Identity
        entity.getPropertyByName('firstName').mapping.mappedForm instanceof PropertyConfig
        entity.getPropertyByName('firstName').mapping.mappedForm.column == 'First_Name'

    }

    void "Test create a Hibernate mapping context for circular one-to-many"() {
        when:
        def mappingContext = new HibernateMappingContext({->
            autowire false
        })
        mappingContext.addPersistentEntities(Task)

        mappingContext.initialize()
        def entity = mappingContext.getPersistentEntity(Task.name)

        then:
        entity instanceof HibernatePersistentEntity
        entity.getPropertyByName('task') instanceof ManyToOne
        entity.getPropertyByName('task').isBidirectional()
        entity.getPropertyByName('task').isCircular()

    }

    void "Test nullable state"() {
        when:
        def mappingContext = new HibernateMappingContext({->
            autowire false
        })
        mappingContext.addPersistentEntities(TestEntity, ChildEntity)

        mappingContext.initialize()
        def entity = mappingContext.getPersistentEntity(TestEntity.name)

        then:
        entity instanceof HibernatePersistentEntity
//        entity.getPropertyByName('child') instanceof ManyToOne
        entity.getPropertyByName('child').isNullable()
        !entity.getPropertyByName('child').mapping.mappedForm.isLazy()
        entity.getPropertyByName('child').mapping.mappedForm.getLazy() == null

    }

    void "test abstract inheritance"() {
        when:
        def mappingContext = new HibernateMappingContext({->
            autowire false
        })
        mappingContext.addPersistentEntities(OnlinePayment, DebitCardPayment, AbstractPayment)

        mappingContext.initialize()
        def parent = mappingContext.getPersistentEntity(AbstractPayment.name)
        def child = mappingContext.getPersistentEntity(DebitCardPayment.name)

        then:
        parent instanceof HibernatePersistentEntity
        parent.isRoot()
        parent.parentEntity == null
        !child.isRoot()
        child.parentEntity == parent
    }

    void "test mappedBy relationships inheritance"() {
        when:
        def mappingContext = new HibernateMappingContext({->
            autowire false
        })
        mappingContext.addPersistentEntities(ManyToManyMappedByBook, ManyToManyMappedByAuthor)

        mappingContext.initialize()
        def book = mappingContext.getPersistentEntity(ManyToManyMappedByBook.name)
        def author = mappingContext.getPersistentEntity(ManyToManyMappedByAuthor.name)

        then:
        book instanceof HibernatePersistentEntity
        author instanceof HibernatePersistentEntity
        !book.getPropertyByName('creator').isBidirectional()
        book.getPropertyByName('creator').inverseSide == null
        book.getPropertyByName('d').isBidirectional()

        Association otherSide = book.getPropertyByName('d')
        otherSide != null
        Association ownerSide = author.getPropertyByName('books')
        ownerSide instanceof ManyToMany
        otherSide instanceof ManyToMany
        ownerSide.isOwningSide()
        !otherSide.isOwningSide()
        ownerSide.isBidirectional()
        ownerSide.inverseSide.name == otherSide.name
        otherSide.isBidirectional()
        otherSide.inverseSide.name == ownerSide.name

    }

    void 'test one-to-one and one-to-many bidirectional'() {
        when:
        def mappingContext = new HibernateMappingContext({->
            autowire false
        })
        mappingContext.addPersistentEntities(BidirectionalOneToManyAndOneToOneMembership, BidirectionalOneToManyAndOneToOneUser)

        mappingContext.initialize()
        def membership = mappingContext.getPersistentEntity(BidirectionalOneToManyAndOneToOneMembership.name)
        def user = mappingContext.getPersistentEntity(BidirectionalOneToManyAndOneToOneUser.name)

        then:
        membership instanceof HibernatePersistentEntity
        user instanceof HibernatePersistentEntity
        !membership.getPropertyByName('referrals').isBidirectional()
        membership.getPropertyByName('referrals').inverseSide == null
        membership.getPropertyByName('user').isBidirectional()

        Association otherSide = membership.getPropertyByName('user')
        otherSide != null
        Association ownerSide = user.getPropertyByName('membership')
        ownerSide instanceof ToOne
        otherSide instanceof ToOne
        ownerSide.isOwningSide()
        !otherSide.isOwningSide()
        ownerSide.isBidirectional()
        ownerSide.inverseSide.name == otherSide.name
        otherSide.isBidirectional()
    }

    void "test circular many-to-many"() {
        when:
        def mappingContext = new HibernateMappingContext({->
            autowire false
        })

        mappingContext.initialize()
        def org = mappingContext.addPersistentEntity(CircularManyToManyOrganization)


        Association children = org.getPropertyByName('children')
        Association parent = org.getPropertyByName('parent')
        Association related = org.getPropertyByName('relatedOrganizations')

        then:
        parent.isBidirectional()
        parent.isCircular()
        parent.inverseSide.name == 'children'
        parent.inverseSide.inverseSide.name == 'parent'
        parent instanceof ManyToOne

        children.isBidirectional()
        children.inverseSide.name == 'parent'
        children.inverseSide.inverseSide.name == 'children'
        children.isCircular()
        children instanceof OneToMany

        related.isBidirectional()
        related.isCircular()
        related.inverseSide.name == 'relatedOrganizations'
        related.inverseSide.inverseSide.name == 'relatedOrganizations'
        related instanceof ManyToMany
    }

    void "Test circular one-to-many and non-circular one-to-many"() {
        when:
        def mappingContext = new HibernateMappingContext({->
            autowire false
        })

        mappingContext.addPersistentEntities(BidirectionalOneToManyAndCircularOneToManyUser,BidirectionalOneToManyAndCircularOneToManyUploadLog,BidirectionalOneToManyAndCircularOneToManyUpload )
        mappingContext.initialize()


        PersistentEntity userClass = mappingContext.getPersistentEntity(BidirectionalOneToManyAndCircularOneToManyUser.name)
        PersistentEntity uploadClass = mappingContext.getPersistentEntity(BidirectionalOneToManyAndCircularOneToManyUpload.name)
        PersistentEntity uploadLogClass = mappingContext.getPersistentEntity(BidirectionalOneToManyAndCircularOneToManyUploadLog.name)

        then:
        userClass.getPropertyByName("users").isBidirectional()
        userClass.getPropertyByName("users").isCircular()

        def uploadsProperty = userClass.getPropertyByName("uploads")

        uploadsProperty.isBidirectional()
        uploadsProperty instanceof OneToMany

        def recipientProperty = uploadClass.getPropertyByName("recipient")

        recipientProperty.isBidirectional()


        uploadsProperty.inverseSide.name == recipientProperty.name
        recipientProperty.inverseSide.name == uploadsProperty.name
        recipientProperty instanceof ManyToOne

        def uploadLogsProperty = userClass.getPropertyByName("uploadLogs")

        uploadLogsProperty.isBidirectional()
        uploadLogsProperty instanceof OneToMany

        def senderProperty = uploadLogClass.getPropertyByName("sender")

        senderProperty.isBidirectional()
        senderProperty instanceof ManyToOne

        uploadLogsProperty.inverseSide.name == senderProperty.name
        senderProperty.inverseSide.name == uploadLogsProperty.name

        def uploadSenderProperty = uploadClass.getPropertyByName("sender")

        uploadSenderProperty instanceof OneToOne
        !uploadSenderProperty.isBidirectional()
    }

    void "test two unidirectional one-to-many associations"() {
        when:
        def mappingContext = new HibernateMappingContext({->
            autowire false
        })

        mappingContext.initialize()
        def org = mappingContext.addPersistentEntity(TwoCircularUnidirectionalOneToManyUser)


        Association children = org.getPropertyByName('children')
        Association parents = org.getPropertyByName('parents')

        then:
        !parents.isBidirectional()
        parents.isCircular()
        parents instanceof OneToMany

        !children.isBidirectional()
        children.isCircular()
        children instanceof OneToMany
    }

    void "Test unidirectional one-to-many and bidirectional one-to-many"() {
        when:
        def mappingContext = new HibernateMappingContext({->
            autowire false
        })

        mappingContext.addPersistentEntities(BidirectionalOneToManyAndUnidirectionalOneToManyGroup,BidirectionalOneToManyAndUnidirectionalOneToManyUser)
        mappingContext.initialize()

        PersistentEntity groupDomain = mappingContext.getPersistentEntity(BidirectionalOneToManyAndUnidirectionalOneToManyGroup.name)
        PersistentEntity userDomain = mappingContext.getPersistentEntity(BidirectionalOneToManyAndUnidirectionalOneToManyUser.name)

        then:
        !groupDomain.getPropertyByName("members").isBidirectional()
        groupDomain.getPropertyByName("members") instanceof OneToMany


        groupDomain.getPropertyByName("owner") instanceof ManyToOne
        groupDomain.getPropertyByName("owner").isBidirectional()

        userDomain.getPropertyByName("groups") instanceof OneToMany
        userDomain.getPropertyByName("groups").isBidirectional()
    }

    void "test tree with list"() {
        when:
        def mappingContext = new HibernateMappingContext({->
            autowire false
        })

        mappingContext.initialize()
        def customer = mappingContext.addPersistentEntity(TreeListCustomer)


        Association children = customer.getPropertyByName('children')
        Association parent = customer.getPropertyByName('parent')

        then:
        parent.isBidirectional()
        parent.isCircular()
        parent.inverseSide.name == children.name
        parent instanceof ManyToOne

        children.isBidirectional()
        children.isCircular()
        children instanceof OneToMany
        children.inverseSide.name == parent.name
    }

}

@Entity
class TreeListCustomer {

    Long id
    Long version

    TreeListCustomer parent
    List children
    String description

    static belongsTo = [parent:TreeListCustomer]
    static hasMany = [children:TreeListCustomer]

    static constraints = {
        parent(nullable: true)
        children(nullable: true)
    }
}


@Entity
class BidirectionalOneToManyAndUnidirectionalOneToManyGroup {

    Long id
    Long version

    // Every user has a bunch of groups that it owns
    BidirectionalOneToManyAndUnidirectionalOneToManyUser owner
    static belongsTo = [ owner : BidirectionalOneToManyAndUnidirectionalOneToManyUser ]

    // In addition, every group has members that are users
    Set members
    static hasMany = [ members : BidirectionalOneToManyAndUnidirectionalOneToManyUser ]
}

@Entity
class BidirectionalOneToManyAndUnidirectionalOneToManyUser {
    Long id
    Long version

    // Every user has a bunch of groups that it owns
    Set groups
    static hasMany = [ groups: BidirectionalOneToManyAndUnidirectionalOneToManyGroup ]
    static mappedBy = [ groups:"owner" ]
}
@Entity
class TwoCircularUnidirectionalOneToManyUser {

    Long id
    Long version
    Set children
    Set parents

    static hasMany = [parents: TwoCircularUnidirectionalOneToManyUser,children: TwoCircularUnidirectionalOneToManyUser]
    String name

    def addChild(u) {
        u.addToParents(this)
        addToChildren(u)
    }

    def addParent(u) {
        u.addToChildren(this)
        addToParents(u)
    }

    String toString() { return name }
}


@Entity
class BidirectionalOneToManyAndCircularOneToManyUser implements java.io.Serializable {
    Long id
    Long version

    static hasMany = [users: BidirectionalOneToManyAndCircularOneToManyUser,
                      uploads: BidirectionalOneToManyAndCircularOneToManyUpload,
                      uploadLogs: BidirectionalOneToManyAndCircularOneToManyUploadLog]
    Set<BidirectionalOneToManyAndCircularOneToManyUser> users
    Set<BidirectionalOneToManyAndCircularOneToManyUpload> uploads
    Set uploadLogs
    static mappedBy = [uploads: 'recipient', uploadLogs: 'sender', users:'manager']
    BidirectionalOneToManyAndCircularOneToManyUser manager
}

@Entity
class BidirectionalOneToManyAndCircularOneToManyUploadLog implements Serializable {
    Long id
    Long version
    static belongsTo = BidirectionalOneToManyAndCircularOneToManyUser
    BidirectionalOneToManyAndCircularOneToManyUser sender
}

@Entity
class BidirectionalOneToManyAndCircularOneToManyUpload implements Serializable {
    Long id
    Long version
    static belongsTo = [BidirectionalOneToManyAndCircularOneToManyUser]

    BidirectionalOneToManyAndCircularOneToManyUser sender
    BidirectionalOneToManyAndCircularOneToManyUser recipient
}


@Entity
class CircularManyToManyOrganization {
    Long id
    Long version

    Set children
    Set relatedOrganizations
    static hasMany = [children: CircularManyToManyOrganization, relatedOrganizations: CircularManyToManyOrganization]
    static mappedBy = [children: "parent", relatedOrganizations:"relatedOrganizations"]

    CircularManyToManyOrganization parent
    String name
}

@Entity
class BidirectionalOneToManyAndOneToOneMembership{
    Long id
    Long version

    String a = 'b'
    Set referrals
    BidirectionalOneToManyAndOneToOneUser user
    Date dateCreated
    static hasMany = [ referrals : BidirectionalOneToManyAndOneToOneUser ]
    static belongsTo = [ user: BidirectionalOneToManyAndOneToOneUser ]

}

@Entity
class BidirectionalOneToManyAndOneToOneUser{
    Long id
    Long version

    String name
    BidirectionalOneToManyAndOneToOneMembership membership

    static mappedBy = [membership:"user"]
}
@Entity
class ManyToManyMappedByBook implements Serializable{
    Long id
    Long version
    String title

    ManyToManyMappedByAuthor creator

    Set d
    static hasMany = [d: ManyToManyMappedByAuthor]
    static belongsTo = [ManyToManyMappedByAuthor]
}

@Entity
class ManyToManyMappedByAuthor implements Serializable {
    Long id
    Long version
    String email
    Set books
    static hasMany = [books: ManyToManyMappedByBook]
    static mappedBy = [books:'d']
}


@Entity
class DebitCardPayment extends AbstractPayment {
    String bank
}
@Entity
class OnlinePayment extends AbstractPayment {
    String service
}


@Entity
abstract class AbstractPayment {
    Long id
    Long version
    Double amount

    static mapping = {
        tablePerConcreteClass true
    }
}



@Entity
class Task implements Serializable {
    Long id
    Long version
    Set tasks
    Task task
    String name

    static mapping = {
        name index:true
    }

    static hasMany = [tasks:Task]
}

@Entity
class PersonDSL {
    Long id
    Long version

    String firstName

    Set children, cousins
    static mapping = {
        table 'people'
        version false
        cache usage:'read-only', include:'non-lazy'
        id generator:'hilo', params:[table:'hi_value',column:'next_value',max_lo:100]

        columns {
            firstName column:'First_Name'
        }
    }
}

@Entity
class TestEntity implements Serializable {
    Long id
    Long version
    String name
    Integer age = 30

    ChildEntity child

    static mapping = {
        name index:true
        age index:true, nullable:true
        child index:true, nullable:true
    }

    static constraints = {
        name blank:false
        child nullable:true
    }
}
@Entity
class ChildEntity implements Serializable {
    Long id
    Long version
    String name

    static mapping = {
        name index:true
    }

    static belongsTo = [TestEntity]
}