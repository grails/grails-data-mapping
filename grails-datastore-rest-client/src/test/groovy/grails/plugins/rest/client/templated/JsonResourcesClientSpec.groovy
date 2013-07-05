package grails.plugins.rest.client.templated

import grails.plugins.rest.client.RestBuilder
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import spock.lang.Specification

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

/**
 * @author Graeme Rocher
 */
class JsonResourcesClientSpec extends Specification {

    void "Test that the get method submits a GET request to the correct URL"() {
        setup:
            RestBuilder restBuilder = new RestBuilder()
            final mockServer = MockRestServiceServer.createServer(restBuilder.restTemplate)
            mockServer.expect(requestTo("http://localhost:8080/books/1")).andExpect(method(HttpMethod.GET)).andRespond(withSuccess('{"title":"The Stand"}', MediaType.APPLICATION_JSON))


        when:"A new JsonResourceClient is constructed and the get method invoked"
            def jsonResourceClient = new JsonResourcesClient("http://localhost:8080/books", restBuilder)
            final result = jsonResourceClient.get(1)

        then:"The result is correct"
            result != null
            result instanceof JSONObject
            result.title == "The Stand"

    }


    void "Test that the delete method submits a DELETE request to the correct URL"() {
        setup:
            RestBuilder restBuilder = new RestBuilder()
            final mockServer = MockRestServiceServer.createServer(restBuilder.restTemplate)
            mockServer.expect(requestTo("http://localhost:8080/books/1")).andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.OK))

        when:"A new JsonResourceClient is constructed and the get method invoked"
            def jsonResourceClient = new JsonResourcesClient("http://localhost:8080/books", restBuilder)
            final result = jsonResourceClient.delete(1)

        then:"The result is correct"
            result != null
            result.statusCode == HttpStatus.OK

    }

    void "Test that the head method with an id submits a HEAD request to the correct URL"() {
        setup:
            RestBuilder restBuilder = new RestBuilder()
            final mockServer = MockRestServiceServer.createServer(restBuilder.restTemplate)
            mockServer.expect(requestTo("http://localhost:8080/books/1")).andExpect(method(HttpMethod.HEAD)).andRespond(withStatus(HttpStatus.OK))


        when:"A new JsonResourceClient is constructed and the get method invoked"
        def jsonResourceClient = new JsonResourcesClient("http://localhost:8080/books", restBuilder)
        final result = jsonResourceClient.head(1)

        then:"The result is correct"
        result != null
        result.statusCode == HttpStatus.OK

    }

    void "Test that the head method submits a HEAD request to the correct URL"() {
        setup:
            RestBuilder restBuilder = new RestBuilder()
            final mockServer = MockRestServiceServer.createServer(restBuilder.restTemplate)
            mockServer.expect(requestTo("http://localhost:8080/books")).andExpect(method(HttpMethod.HEAD)).andRespond(withStatus(HttpStatus.OK))

        when:"A new JsonResourceClient is constructed and the get method invoked"
            def jsonResourceClient = new JsonResourcesClient("http://localhost:8080/books", restBuilder)
            final result = jsonResourceClient.head()

       then:"The result is correct"
            result != null
            result.statusCode == HttpStatus.OK

    }

    void "Test that the post method correctly sends JSON to the appropriate URL"() {
        setup:
            RestBuilder restBuilder = new RestBuilder()
            final mockServer = MockRestServiceServer.createServer(restBuilder.restTemplate)
            mockServer.expect(requestTo("http://localhost:8080/books"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string('{"title":"The Stand"}'))
                    .andExpect(content().contentType("application/json"))
                    .andRespond(withStatus(HttpStatus.CREATED))

        when:"A new JsonResourceClient is constructed and the get method invoked"
            def jsonResourceClient = new JsonResourcesClient("http://localhost:8080/books", restBuilder)
            final result = jsonResourceClient.post(new Book(title:"The Stand"))

        then:"The result is correct"
            result != null
            result.statusCode == HttpStatus.CREATED

    }
}
class Book {
    String title
}
