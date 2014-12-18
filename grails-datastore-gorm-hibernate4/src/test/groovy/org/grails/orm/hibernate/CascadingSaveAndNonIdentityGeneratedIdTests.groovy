package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

class CascadingSaveAndNonIdentityGeneratedIdTests extends AbstractGrailsHibernateTests {

    @Test
    void testCascadingSaveToMultipleLevels() {

        def request = CascadingSaveAndNonIdentityGeneratedIdRequest.newInstance()

        10.times {
            def sample = new CascadingSaveAndNonIdentityGeneratedIdSample()
            request.addToSamples(sample)
            10.times {
                sample.addToAttributes(new CascadingSaveAndNonIdentityGeneratedIdAttribute())
            }
        }

        request.save(flush:true)
        session.clear()

        assert !request.errors.hasErrors()
        assert request.id

        def savedRequest = CascadingSaveAndNonIdentityGeneratedIdRequest.get(request.id)
        assert savedRequest.samples.size() == 10
        savedRequest.samples.each {
            assert it.attributes.size() == 10
        }
    }

    @Override
    protected getDomainClasses() {
        [CascadingSaveAndNonIdentityGeneratedIdRequest, CascadingSaveAndNonIdentityGeneratedIdSample, CascadingSaveAndNonIdentityGeneratedIdAttribute]
    }
}


@Entity
class CascadingSaveAndNonIdentityGeneratedIdRequest {
    Long version
    Long id

    Set samples
    static hasMany = [samples: CascadingSaveAndNonIdentityGeneratedIdSample]

    Date dateCreated
    Date lastUpdated

    static mapping = {
        cache false
        id generator: 'increment'
    }
}

@Entity
class CascadingSaveAndNonIdentityGeneratedIdSample {
    Long version
    Long id

    CascadingSaveAndNonIdentityGeneratedIdRequest request
    static belongsTo = [request : CascadingSaveAndNonIdentityGeneratedIdRequest]
    Set attributes
    static hasMany = [attributes: CascadingSaveAndNonIdentityGeneratedIdAttribute]

    static mapping = {
        cache false
        id generator: 'increment'
    }
}

@Entity
class CascadingSaveAndNonIdentityGeneratedIdAttribute {
    Long version
    Long id

    CascadingSaveAndNonIdentityGeneratedIdSample sample

    static belongsTo = [sample : CascadingSaveAndNonIdentityGeneratedIdSample]

    static mapping = {
        cache false
        id generator: 'increment'
    }
}
