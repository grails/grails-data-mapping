package org.grails.datastore.gorm

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity

import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.springframework.validation.Errors
import org.springframework.validation.Validator

import spock.lang.Issue

class UniqueConstraintGroupSpec extends GormDatastoreSpec{

    @Issue(['GRAILS-8656', 'GRAILS-10178'])
    void "Test null uniqueness handling"() {

        given:"Some test users"
            UserClass user1 = createTestUser(1)
            UserClass user2 = createTestUser(1)

            user1.save(flush:true)

        when:"another user with same userId"
            user2.validate()
            def errors = user2.errors.allErrors

        then:"(null) dateDeleted should pass uniqueness test"
            errors.size() == 0

        when: "another user with same userId, and different (not null) dateDeleted is saved"
            user2.dateDeleted  = new Date()

        then:"validation passes"
            user2.validate()

        when: "Now check the same when user1 has not null dateDeleted"
            user1.dateDeleted = user2.dateDeleted
        then:"The save succeeds"
            user1.save(flush:true)

        when: "user1 and user 2 have same userid / dateDeleted"
            user2.validate()
            errors = user2.errors.allErrors

        then:"should fail uniqueness"
            errors.size() == 1
            errors[0].field == "userId"
            errors[0].code == "unique"

        when: "user 2 has not date deleted different from user 1"
            user2.dateDeleted = (user1.dateDeleted + 1)
        then: "passes uniqueness"
            user2.validate()

        when: "user 2 has null date deleted"
            user2.dateDeleted = null
        then: "passes uniqueness"
            user2.validate()
    }

    @Override
    List getDomainClasses() {
        [UserClass]
    }

    void setup() {

        def groupValidator = [supports: {Class cls -> true},
                validate: {Object target, Errors errors ->
                    def constrainedProperties = GrailsDomainConfigurationUtil.evaluateConstraints(UserClass)
                    for (ConstrainedProperty cp in constrainedProperties.values()) {
                        cp.validate(target, target[cp.propertyName], errors)
                    }
                }] as Validator

        final context = session.datastore.mappingContext
        final entity = context.getPersistentEntity(UserClass.name)
        context.addEntityValidator(entity, groupValidator)
    }

    private createTestUser(Integer suffix) {
        def val = new UserClass()
        val.dateDeleted = null
        val.userId = "UserId${suffix}"
        val
    }
}

@Entity
class UserClass {

    Long id
    String userId
    Date dateDeleted

    static constraints = {
        userId blank: false, unique: 'dateDeleted'
        dateDeleted nullable: true
    }
}
