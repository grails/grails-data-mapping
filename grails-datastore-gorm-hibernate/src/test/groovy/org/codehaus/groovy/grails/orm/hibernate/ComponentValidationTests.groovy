package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.validation.AbstractConstraint
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.junit.Before
import org.junit.Test
import org.springframework.validation.Errors

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 29, 2008
 */
class ComponentValidationTests extends AbstractGrailsHibernateTests {

    @Before
    void setup() {
        ConstrainedProperty.removeConstraint("custom")
    }

    @Test
    void testComponentValidation() {

        ConstrainedProperty.removeConstraint("custom")
        ga.refreshConstraints()

        assert !ConstrainedProperty.hasRegisteredConstraint("custom")

        def person = new ComponentValidationTestsPerson()
        person.name = 'graeme'
        def date = new Date()
        person.auditInfo = new ComponentValidationTestsAuditInfo(dateEntered:date,dateUpdated:date,enteredBy:'chris',updatedBy:'chris')


        def isValid = person.validate()
        println "ERRORS = ${person.errors}"
        assert isValid
    }


    @Test
    void testCustomConstraint() {



        // Register the new constraint.
        ConstrainedProperty.registerNewConstraint("custom", CustomConstraint)

        // Refresh the constraints now that we have registered a new one.
        ga.refreshConstraints()

        // Create the test domain instances.
        def person = new ComponentValidationTestsPerson()
        def date = new Date()
        person.name = 'graeme'
        person.auditInfo = new ComponentValidationTestsAuditInfo(
                dateEntered: date,
                dateUpdated: date,
                enteredBy: 'chris',
                updatedBy: 'chris')

        assertFalse "The validation should fail since the custom validator has been registered.", person.validate()
    }

    @Override
    protected getDomainClasses() {
        [ComponentValidationTestsPerson, ComponentValidationTestsAuditInfo]
    }
}

class ComponentValidationTestsPerson {
    Long id
    Long version
    String name

    ComponentValidationTestsAuditInfo auditInfo
    static embedded = ['auditInfo']

    static constraints = {
        name(nullable:false, maxSize:35)
    }
}

class ComponentValidationTestsAuditInfo {
    Long id
    Long version

    Date dateEntered
    Date dateUpdated
    String enteredBy
    String updatedBy

    static constraints = {
        dateEntered(nullable:false)
        dateUpdated(nullable:false)
        enteredBy(nullable:false,maxSize:20,custom: true)
        updatedBy(nullable:false,maxSize:20)
    }

    String toString() {
        "$enteredBy $dateEntered $updatedBy $dateUpdated"
    }
}
class CustomConstraint extends AbstractConstraint {
    boolean active
    String name = "custom"

    void setParameter(Object constraintParameter) {
        assert constraintParameter instanceof Boolean

        active = constraintParameter.booleanValue()
        super.setParameter(constraintParameter)
    }

    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        if (active) {
            if (propertyValue != "fred") {
                def args = [constraintPropertyName, constraintOwningClass, propertyValue] as Object[]
                super.rejectValue(target, errors, "some.error.message", "invalid.custom", args)
            }
        }
    }

    boolean supports(Class type) {
        type == String
    }
}