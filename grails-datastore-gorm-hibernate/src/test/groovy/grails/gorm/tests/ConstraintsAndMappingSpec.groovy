package grails.gorm.tests

import spock.lang.Issue
import grails.persistence.Entity
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.orm.hibernate.cfg.HibernateMappingBuilder

/**
 */
class ConstraintsAndMappingSpec extends GormDatastoreSpec{


    @Issue('GRAILS-10207')
    void "Test that defining a mapping block doesn't disable the unique constraint"() {
        when:"The mapping is obtained"
            GrailsApplication grailsApplication = session.datastore.mappingContext.grailsApplication
            GrailsDomainClass domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, ConstraintsAndMappingAccount.name)
            ConstrainedProperty cp = domainClass.constrainedProperties['user']

        then:"The unique constraint to be applied"
            cp.getMetaConstraintValue("unique")
    }
    @Override
    List getDomainClasses() {
        [ConstraintsAndMappingUser, ConstraintsAndMappingAccount]
    }
}

@Entity
class ConstraintsAndMappingUser {

    String name

    static mapping = {
        table name:'user_table'
    }

}

@Entity
class ConstraintsAndMappingAccount {

    String description
    ConstraintsAndMappingUser user

    static constraints = {
        description unique: true
        user unique: true
    }

    // Comment out the mapping block and the unique constraint will be applied:
    static mapping = {
        description column: 'descriptionx'
        user column: 'userx'
    }

}
