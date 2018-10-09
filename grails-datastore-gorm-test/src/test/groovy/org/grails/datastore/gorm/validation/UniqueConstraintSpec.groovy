package org.grails.datastore.gorm.validation


import grails.gorm.annotation.Entity
import grails.gorm.transactions.Transactional
import org.grails.datastore.gorm.validation.constraints.MappingContextAwareConstraintFactory
import org.grails.datastore.gorm.validation.constraints.builtin.UniqueConstraint
import org.grails.datastore.gorm.validation.constraints.registry.ConstraintRegistry
import org.grails.datastore.gorm.validation.constraints.registry.DefaultValidatorRegistry
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.springframework.context.support.StaticMessageSource
import spock.lang.AutoCleanup
import spock.lang.Specification

@Transactional
class UniqueConstraintSpec extends Specification {

    @AutoCleanup SimpleMapDatastore datastore = new SimpleMapDatastore(
            Channel,
            DefaultChannel,
            ListChannel,
            Organization
    )

    def setup() {
        ConstraintRegistry constraintRegistry = new DefaultValidatorRegistry(
                datastore.mappingContext,
                datastore.connectionSources.defaultConnectionSource.settings
        )

        def messageSource = new StaticMessageSource()
        constraintRegistry.addConstraintFactory(
                new MappingContextAwareConstraintFactory(UniqueConstraint.class, messageSource, datastore.mappingContext)
        )

        datastore.mappingContext.setValidatorRegistry(constraintRegistry)
    }

    void 'unique constraint works with parent/child'() {
        given: 'an existing channel'
        def testOrg = new Organization(name: 'Test 1').save(failOnError: true)
        def defaultChannel1 = new DefaultChannel(name: 'General', organization: testOrg).save(failOnError: true, flush: true)

        when: 'a new channel with the same name is created'
        def defaultChannel2 = new DefaultChannel(name: defaultChannel1.name, organization: testOrg)

        then:
        !defaultChannel2.validate()
        defaultChannel2.hasErrors()
        defaultChannel2.errors.getFieldError('name').code == 'unique'

    }

    void "unique constraint works in sibling classes"() {
        given: 'an existing channel'
        def testOrg = new Organization(name: 'Test 1').save(failOnError: true)
        def defaultChannel = new DefaultChannel(name: 'General', organization: testOrg).save(failOnError: true, flush: true)

        when: 'a new channel with the same name is created'
        def listChannel = new ListChannel(name: defaultChannel.name, organization: testOrg)

        then:
        !listChannel.validate()
        listChannel.hasErrors()
        listChannel.errors.getFieldError('name').code == 'unique'

        when: 'the channel belongs to another org'
        listChannel.clearErrors()
        listChannel.organization = new Organization(name: 'Test 2').save(failOnError: true)

        then:
        listChannel.validate()
    }
}


@Entity
abstract class Channel {

    String name
    static belongsTo = [organization: Organization]

    static constraints = {
        name size: 1..80, unique: 'organization'
    }
}

@Entity
class DefaultChannel extends Channel {

    static constraints = {
    }
}

@Entity
class ListChannel extends Channel {

    static constraints = {
    }
}

@Entity
class Organization {
    String name
}