package org.grails.datastore.gorm.rest.client.json

import grails.gorm.dirty.checking.DirtyCheck
import grails.persistence.Entity
import org.codehaus.groovy.grails.web.servlet.HttpHeaders
import org.grails.datastore.gorm.rest.client.RestClientDatastoreSpec
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

/**
 */
class DirtyCheckingSpec extends RestClientDatastoreSpec{

    void "Test dirty checking prevents unnecessary updates"() {
        when:"An object is read"
            RestTemplate rt = Author.getRestBuilder().restTemplate
            def mockServer = MockRestServiceServer.createServer(rt)
            mockServer.expect(requestTo("http://localhost:8080/author/1"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON.toString()))
                    .andRespond(withSuccess('{"id":1, "name":"Stephen King"}', MediaType.APPLICATION_JSON))

            Author author = Author.get(1)

        then:"The instance is initially not dirty"
            author instanceof DirtyCheckable
            !author.isDirty()
            author.id == 1L

        when:"The entity is saved then no PUT is issued because it isn't dirty"
            mockServer = MockRestServiceServer.createServer(rt)
            author.save(flush:true)

        then:'Verify no requests were sent'
            mockServer.verify()

        when:"The entity is changed"
            author.name = "James Patterson"
            mockServer.expect(requestTo("http://localhost:8080/author/1"))
                    .andExpect(method(HttpMethod.PUT))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(content().string('{"class":"org.grails.datastore.gorm.rest.client.json.Author","id":1,"name":"James Patterson"}'))
                    .andRespond(withSuccess('{"id":1, "name":"James Patterson"}', MediaType.APPLICATION_JSON))
            author.save(flush: true)

        then:"a PUT request should be issued for the dirty instance"
            mockServer.verify()

        when:"The entity is saved then no PUT is issued because it isn't dirty"
            mockServer = MockRestServiceServer.createServer(rt)
            author.save(flush:true)

        then:'Verify no requests were sent'
            mockServer.verify()




    }
    @Override
    List<Class> getDomainClasses() {
        [Author]
    }
}

@DirtyCheck
@Entity
class Author {
    Long id
    String name

    static mapping = {
//        async false
    }
}
