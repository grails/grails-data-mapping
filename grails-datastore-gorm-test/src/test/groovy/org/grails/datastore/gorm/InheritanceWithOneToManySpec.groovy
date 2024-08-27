package org.grails.datastore.gorm

import spock.lang.Ignore

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity

import spock.lang.Issue

@Ignore("https://issues.apache.org/jira/browse/GROOVY-5106 - The interface GormEntity cannot be implemented more than once with different arguments: org.grails.datastore.gorm.GormEntity<grails.gorm.tests.XXX> and org.grails.datastore.gorm.GormEntity<grails.gorm.tests.XXX>")
class InheritanceWithOneToManySpec extends GormDatastoreSpec{

    @Issue('GRAILS-9010')
    void "Test that a one-to-many cascades to an association featuring inheritance"() {
        when:"A domain model with an association featuring inheritance is saved"
        def group = new Group(name:"my group")
        def subMember = new SubMember(name:"my name",extraName:"extra name",externalId:'blah')
        group.addToMembers subMember
        group.save(failOnError:true, flush:true)
        session.clear()

        then:"The association is correctly saved"
        Group.count() == 1
        SubMember.count() == 1
    }

    @Override
    List getDomainClasses() {
        [Group, Member, SubMember]
    }
}

@Entity
class Group {
    Long id
    String name
    static hasMany = [members:Member]
    Collection members
}

//@Entity
class Member   {
    Long id
    String name
    String externalId
}

@Entity
class SubMember extends Member {
    String extraName
}