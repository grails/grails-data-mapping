package grails.plugins.rest.client

import grails.async.Promise
import grails.converters.JSON
import grails.plugins.rest.client.async.AsyncRestBuilder
import grails.web.JSONBuilder
import groovy.util.slurpersupport.GPathResult
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.Issue
import spock.lang.Specification

import static org.hamcrest.CoreMatchers.anything
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.http.HttpMethod

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.codehaus.groovy.grails.web.servlet.HttpHeaders

import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.http.MediaType
import org.springframework.http.HttpStatus

/**
 * @author Graeme Rocher
 */
class AsyncRestBuilderSpec extends Specification{
    def "Test that a basic GET request returns a JSON result of the response type is JSON"(){
        given:"A rest client instance"
            def rest = new AsyncRestBuilder()
            final mockServer = MockRestServiceServer.createServer(rest.restTemplate)
            mockServer.expect(requestTo("http://grails.org/api/v1.0/plugin/acegi/"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess('{"name":"acegi"}', MediaType.APPLICATION_JSON))


        when:"A get request is issued for a response that returns XML"
            Promise<RestResponse> resp = rest.get("http://grails.org/api/v1.0/plugin/acegi/")

        then:"The response is a gpath result"
            resp != null
            resp.get().json instanceof JSONObject
            resp.get().json.name == 'acegi'
    }

    def "Test that obtaining a 404 response doesn't throw an exception but instead returns the response object for inspection"() {
        given:"A rest client instance"
            def rest = new AsyncRestBuilder()
            final mockServer = MockRestServiceServer.createServer(rest.restTemplate)
            mockServer.expect(requestTo("http://grails.org/api/v1.0/plugin/nonsense"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header(HttpHeaders.ACCEPT, "application/xml"))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND))

        when:"A get request is issued to a URL that returns a 404"
            Promise<RestResponse> resp = rest.get("http://grails.org/api/v1.0/plugin/nonsense") {
                accept "application/xml"
            }

        then:"Check the status"
            resp.get().status == 404
            resp.get().text instanceof String
    }

    def "Test that a basic GET request returns a JSON result of the response type is JSON with custom settings"(){
        given:"A rest client instance"
            def rest = new AsyncRestBuilder(connectTimeout:1000, readTimeout:20000)
            final mockServer = MockRestServiceServer.createServer(rest.restTemplate)
            mockServer.expect(requestTo("http://grails.org/api/v1.0/plugin/acegi/"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess('{"name":"acegi"}', MediaType.APPLICATION_JSON))


        when:"A get request is issued for a response that returns XML"
            Promise<RestResponse> resp = rest.get("http://grails.org/api/v1.0/plugin/acegi/")

        then:"The response is a gpath result"
            resp != null
            resp.get().json instanceof JSONObject
            resp.get().json.name == 'acegi'
    }

    def "Test that a basic GET request returns a XML result of the response type is XML"(){
        given:"A rest client instance"
            def rest = new AsyncRestBuilder()
            final mockServer = MockRestServiceServer.createServer(rest.restTemplate)
            mockServer.expect(requestTo("http://grails.org/api/v1.0/plugin/acegi/"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header(HttpHeaders.ACCEPT, "application/xml"))
                    .andRespond(withSuccess('<plugin><name>acegi</name></plugin>', MediaType.APPLICATION_XML))


        when:"A get request is issued for a response that returns XML"
            Promise<RestResponse> resp = rest.get("http://grails.org/api/v1.0/plugin/acegi/") {
                accept 'application/xml'
            }

        then:"The response is a gpath result"
            resp != null
            resp.get().xml instanceof GPathResult
            resp.get().xml.name == 'acegi'
    }

    def "Test basic authentication with GET request"() {
        given:"A rest client instance"
            def rest = new AsyncRestBuilder()
            final mockServer = MockRestServiceServer.createServer(rest.restTemplate)
            mockServer.expect(requestTo("http://repo.grails.org/grails/api/security/users"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header(HttpHeaders.AUTHORIZATION, anything()))
                    .andRespond(withSuccess('["foo", "bar"]', MediaType.APPLICATION_JSON))


        when:"A get request is issued for a response that returns XML"
            Promise<RestResponse> resp = rest.get("http://repo.grails.org/grails/api/security/users"){
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
            }

        then:"The response is a gpath result"
            resp != null
            resp.get().json instanceof JSONArray
    }

    def "Test basic authentication with PUT request"() {
        given:"A rest client instance"
            def rest = new AsyncRestBuilder()
            RestBuilderSpec.mockArtifactoryUserGroupApi(rest.restBuilder)

        when:"A get request is issued for a response that returns XML"
            Promise<RestResponse> resp = rest.put("http://repo.grails.org/grails/api/security/groups/test-group"){
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
                contentType "application/vnd.org.jfrog.artifactory.security.Group+json"
                json {
                    name = "test-group"
                    description = "A temporary test group"
                }
            }
        then:"The response is a gpath result"
            resp != null
            resp.get().status == 201
            resp.get().text == "Created"

        when:"The resource contents are requested"
            resp = rest.get("http://repo.grails.org/grails/api/security/groups/test-group") {
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
            }

        then:"The contents are valid"
            resp != null
            resp.get().json.name == 'test-group'

        when:"The resource is deleted"
            resp = rest.delete("http://repo.grails.org/grails/api/security/groups/test-group") {
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
            }

        then:"The resource is gone"
            resp != null
            resp.get().status == 200
    }

    def "Test basic authentication with PUT request and JSON body"() {
        given:"A rest client instance"
            def rest = new AsyncRestBuilder()
            RestBuilderSpec.mockArtifactoryUserGroupApi(rest.restBuilder)

        when:"A get request is issued for a response that returns XML"
            def builder = new JSONBuilder()
            JSON j = builder.build {
                name = "test-group"
                description = "A temporary test group"
            }
            Promise<RestResponse> resp = rest.put("http://repo.grails.org/grails/api/security/groups/test-group"){
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
                contentType "application/vnd.org.jfrog.artifactory.security.Group+json"
                body j
            }
        then:"The response is a gpath result"
            resp != null
            resp.get().status == 201
            resp.get().text == "Created"

        when:"The resource contents are requested"
            resp = rest.get("http://repo.grails.org/grails/api/security/groups/test-group") {
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
            }

        then:"The contents are valid"
            resp != null
            resp.get().json.name == 'test-group'

        when:"The resource is deleted"
            resp = rest.delete("http://repo.grails.org/grails/api/security/groups/test-group") {
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
            }

        then:"The resource is gone"
            resp != null
            resp.get().status == 200
    }

    def "Test basic authentication with PUT request and JSON as map"() {
        given:"A rest client instance"
            def rest = new AsyncRestBuilder()
            RestBuilderSpec.mockArtifactoryUserGroupApi(rest.restBuilder)

        when:"A get request is issued for a response that returns XML"
            def builder = new JSONBuilder()
            def j = [
                    name : "test-group",
                    description : "A temporary test group"
            ]
            Promise<RestResponse> resp = rest.put("http://repo.grails.org/grails/api/security/groups/test-group"){
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
                contentType "application/vnd.org.jfrog.artifactory.security.Group+json"
                json j
            }
        then:"The response is a gpath result"
            resp != null
            resp.get().status == 201
            resp.get().text == "Created"

        when:"The resource contents are requested"
            resp = rest.get("http://repo.grails.org/grails/api/security/groups/test-group") {
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
            }

        then:"The contents are valid"
            resp != null
            resp.get().json.name == 'test-group'

        when:"The resource is deleted"
            resp = rest.delete("http://repo.grails.org/grails/api/security/groups/test-group") {
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
            }

        then:"The resource is gone"
            resp != null
            resp.get().status == 200
    }


}
