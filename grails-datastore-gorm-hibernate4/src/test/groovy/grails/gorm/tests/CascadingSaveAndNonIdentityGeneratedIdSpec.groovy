package grails.gorm.tests

import grails.persistence.Entity

/**
 * Created by graemerocher on 23/04/14.
 */
class CascadingSaveAndNonIdentityGeneratedIdSpec extends GormDatastoreSpec{

    void "Test cascading save to multiple levels"() {

        when:"A domain with multiple nested levels is saved"
            def request = new Request()

            10.times {
                def sample = new Sample()
                request.addToSamples(sample)
                10.times {
                    sample.addToAttributes(new Attribute())
                }
            }

            request.save(flush:true)
            session.clear()
            def savedRequest = Request.get(request.id)

        then:"The entities are persisted correctly"
            savedRequest.samples.size() == 10
            savedRequest.samples.every {
                it.attributes.size() == 10
            }
    }

    @Override
    List getDomainClasses() {
        [Request, Sample, Attribute]
    }
}

@Entity
class Request {
    Long id
    Long version
    static hasMany = [samples: Sample]
    Set samples

    Date dateCreated
    Date lastUpdated

    static mapping = {
        cache false
        id generator: 'increment'
    }
}

@Entity
class Sample {
    Long id
    Long version
    Request request
    Set attributes
    static belongsTo = [request : Request]
    static hasMany = [attributes: Attribute]

    static mapping = {
        cache false
        id generator: 'increment'
    }
}

@Entity
class Attribute {
    Long id
    Long version
    Sample sample

    static belongsTo = [sample : Sample]

    static mapping = {
        cache false
        id generator: 'increment'
    }
}