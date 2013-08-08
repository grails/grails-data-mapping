package grails.gorm.tests

import grails.persistence.Entity

import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.grails.datastore.gorm.validation.constraints.UniqueConstraint
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.springframework.validation.Errors
import org.springframework.validation.Validator

import javax.persistence.FlushModeType

/**
 * Tests the unique constraint
 */
class UniqueConstraintSpec extends GormDatastoreSpec {

    void "Test simple unique constraint"() {
        given:"A validator that uses the unique constraint"
            setupValidator()

        when:"Two domain classes with the same name are saved"
            def one = new UniqueGroup(name:"foo").save(flush:true)
            def two = new UniqueGroup(name:"foo")
            two.save(flush:true)

        then:"The second has errors"
            one != null
            two.hasErrors()
            UniqueGroup.count() == 1

        when:"The first is saved again"
            one = one.save(flush:true)

        then:"The are no errors"
            one != null

        when:"Three domain classes are saved within different uniqueness groups"
            one = new GroupWithin(name:"foo", org:"mycompany").save(flush:true)
            two = new GroupWithin(name:"foo", org:"othercompany").save(flush:true)
            def three = new GroupWithin(name:"foo", org:"mycompany")
            three.save(flush:true)

        then:"Only the third has errors"
            one != null
            two != null
            three.hasErrors()
            GroupWithin.count() == 2
    }

    void "should update to a existing value fail"() {
        given:"A validator that uses the unique constraint"
            setupValidator()

            new UniqueGroup(name:"foo").save()
            def two = new UniqueGroup(name:"bar").save()

            session.flush()
            session.clear()

        when:
            two.name="foo"
            two.save(flush:true)

        then:
            two.hasErrors()
            UniqueGroup.count() == 2
            UniqueGroup.get(two.id).name=="bar"

        when:
            session.clear()
            two = UniqueGroup.get(two.id)

        then:
            two.name == "bar"

    }

    void "withManualFlushMode should use flushmode commit"() {

        setup:
        def constraint = new UniqueConstraint(session.datastore)
        constraint.owningClass = UniqueGroup

        when: "check if session flushmode has really switched to COMMIT"
        constraint.withManualFlushMode {
            assert session.flushMode == FlushModeType.COMMIT
        }

        then:
        session.flushMode == FlushModeType.AUTO
    }

    protected void setupValidator() {

        def groupValidator = [supports: {Class cls -> true},
            validate: {Object target, Errors errors ->
                def constrainedProperties = GrailsDomainConfigurationUtil.evaluateConstraints(UniqueGroup)
                for (ConstrainedProperty cp in constrainedProperties.values()) {
                    cp.validate(target, target[cp.propertyName], errors)
                }
            }] as Validator

        def groupWithinValidator = [supports: {Class cls -> true},
            validate: {Object target, Errors errors ->
                def constrainedProperties = GrailsDomainConfigurationUtil.evaluateConstraints(GroupWithin)
                for (ConstrainedProperty cp in constrainedProperties.values()) {
                    cp.validate(target, target[cp.propertyName], errors)
                }
            }] as Validator

        final MappingContext context = session.datastore.mappingContext
        final PersistentEntity entity = context.getPersistentEntity(UniqueGroup.name)
        context.addEntityValidator(entity, groupValidator)
        entity = context.getPersistentEntity(GroupWithin.name)
        context.addEntityValidator(entity, groupWithinValidator)
    }

    @Override
    List getDomainClasses() {
        [UniqueGroup, GroupWithin]
    }
}

@Entity
class UniqueGroup implements Serializable {
    Long id
    String name
    static constraints = {
        name unique:true, index:true
    }
}

@Entity
class GroupWithin implements Serializable {
    Long id
    String name
    String org
    static constraints = {
        name unique:"org", index:true
        org index:true
    }
}
