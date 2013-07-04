package grails.plugins.rest.client.templated

import grails.converters.JSON
import grails.plugins.rest.client.RequestCustomizer
import grails.plugins.rest.client.RestBuilder
import org.codehaus.groovy.grails.web.json.JSONElement
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class JsonResourcesClientSpec extends Specification {

    void "Test that the get method submits a GET request to the correct URL"() {
        setup:
            RestBuilder restBuilder = new RestBuilder() {
                @Override
                protected ResponseEntity invokeRestTemplate(String url, HttpMethod method, RequestCustomizer requestCustomizer) {
                    assert url == "http://localhost:8080/books/{id}"
                    assert method == HttpMethod.GET
                    assert requestCustomizer.urlVariables
                    assert requestCustomizer.urlVariables.id == 1
                    assert requestCustomizer.acceptType == JSON
                    assert requestCustomizer.headers.getAccept().contains(MediaType.APPLICATION_JSON)
                    return new ResponseEntity(JSON.parse('{"title":"Good"}'), HttpStatus.OK)
                }
            }

        when:"A new JsonResourceClient is constructed and the get method invoked"
            def jsonResourceClient = new JsonResourcesClient("http://localhost:8080/books", restBuilder)
            final result = jsonResourceClient.get(1)

        then:"The result is correct"
            result != null
            result instanceof JSONElement

    }

    void "Test that the delete method submits a DELETE request to the correct URL"() {
        setup:
        RestBuilder restBuilder = new RestBuilder() {
            @Override
            protected ResponseEntity invokeRestTemplate(String url, HttpMethod method, RequestCustomizer requestCustomizer) {
                assert url == "http://localhost:8080/books/{id}"
                assert method == HttpMethod.DELETE
                return new ResponseEntity(HttpStatus.OK)
            }
        }

        when:"A new JsonResourceClient is constructed and the get method invoked"
            def jsonResourceClient = new JsonResourcesClient("http://localhost:8080/books", restBuilder)
            final result = jsonResourceClient.delete(1)

        then:"The result is correct"
            result != null
            result.statusCode == HttpStatus.OK

    }
}
