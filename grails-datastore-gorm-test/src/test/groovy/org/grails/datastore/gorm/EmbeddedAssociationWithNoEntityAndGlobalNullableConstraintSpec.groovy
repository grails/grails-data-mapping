package org.grails.datastore.gorm

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.validation.PersistentEntityValidator
import grails.persistence.Entity
import org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator
import org.grails.datastore.gorm.validation.constraints.registry.DefaultValidatorRegistry
import org.grails.datastore.mapping.config.Settings
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.springframework.context.MessageSource
import spock.lang.AutoCleanup
import spock.lang.Shared

class EmbeddedAssociationWithNoEntityAndGlobalNullableConstraintSpec extends GormDatastoreSpec {

    @Shared @AutoCleanup SimpleMapDatastore datastore = new SimpleMapDatastore(
        DatastoreUtils.createPropertyResolver((Settings.SETTING_DEFAULT_CONSTRAINTS): { '*'(nullable: true) }),
        [],
        User2
    )

    void "global constraints are applied to embedded properties defined as POGO"() {
        given:
        def user = new User2()
        user.username = 'admin'
        user.address.city = 'Madrid'

        def context = datastore.mappingContext
        context.setValidatorRegistry(new DefaultValidatorRegistry(context, datastore.getConnectionSources().getDefaultConnectionSource().settings))

        PersistentEntity entityUser = datastore.getMappingContext().getPersistentEntity(User2.name)
        def validatorUser = new PersistentEntityValidator(entityUser, Mock(MessageSource), new DefaultConstraintEvaluator())
        session.datastore.mappingContext.addEntityValidator(entityUser, validatorUser)

        PersistentEntity entityAddress = datastore.getMappingContext().getPersistentEntity(User2.name)
        def validatorAddress = new PersistentEntityValidator(entityAddress, Mock(MessageSource), new DefaultConstraintEvaluator())
        session.datastore.mappingContext.addEntityValidator(entityAddress, validatorAddress)

        when:
        user.validate()

        then:
        !user.hasErrors()
    }

    @Override
    List getDomainClasses() {
        [User2]
    }
}

@Entity
class User2 {
    Long id
    String username
    String password
    Address2 address = new Address2()

    static embedded = ['address']

    static mapping = {
    }
}

class Address2 {
    String city
    String postCode
}
