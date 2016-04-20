package grails.gorm.tests

import grails.persistence.Entity
import groovy.transform.NotYetImplemented
import org.grails.orm.hibernate.GormSpec
import spock.lang.Issue

/**
 * Created by graemerocher on 20/04/16.
 */
class CompositeIdAssociationMappingSpec extends GormSpec {

    @Issue('https://github.com/grails/grails-data-mapping/issues/660')
    @NotYetImplemented
    void "Test composite id usage with associations"() {
        expect:
        AChild.count() == 0
        AParent.count() == 0
        GrandParent.count() == 0
    }
    @Override
    List getDomainClasses() {
        [AChild, AParent, GrandParent]
    }
}

@Entity
class AChild implements Serializable {
    AParent parent
    String name

    static belongsTo= [parent: AParent]

    static mapping= {
        id(composite: ['parent', 'name'])
    }
}

@Entity
class AParent implements Serializable {
    GrandParent grandParent
    String name
    Collection<AChild> children

    static belongsTo= [grandParent: GrandParent]
    static hasMany= [children: AChild]

    static mapping= {
        id(composite: ['grandParent', 'name'])
    }
}

@Entity
class GrandParent implements Serializable {
    String name
    Integer luckyNumber
    Collection<AParent> parents

    static hasMany= [parents: AParent]

    static mapping= {
        id(composite: ['name', 'luckyNumber'])
    }
}