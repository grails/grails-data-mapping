package grails.plugins.rest.client

import grails.converters.JSON
import grails.web.JSONBuilder
import grails.web.http.HttpHeaders
import groovy.util.slurpersupport.GPathResult
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import spock.lang.Issue
import spock.lang.Specification

import static org.hamcrest.CoreMatchers.anything
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

class RestBuilderSpec extends Specification {


    def "Test proxy configuration"() {
        when:"RestBuilder is configured with proxy settings"
            def rest = new RestBuilder(proxy:['localhost':8888])
            def proxyAddress = rest.restTemplate.requestFactory?.@proxy?.address()

        then:"The proxy settings are correct"
            proxyAddress != null
            proxyAddress.hostName == "localhost"
            proxyAddress.port == 8888
    }

    def "Test that a basic GET request returns a JSON result of the response type is JSON"(){
        given:"A rest client instance"
            def rest = new RestBuilder()
            final mockServer = MockRestServiceServer.createServer(rest.restTemplate)
            mockServer.expect(requestTo("http://grails.org/api/v1.0/plugin/acegi/"))
                       .andExpect(method(HttpMethod.GET))
                       .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                        .andRespond(withSuccess('{"name":"acegi"}', MediaType.APPLICATION_JSON))


        when:"A get request is issued for a response that returns XML"
            def resp = rest.get("http://grails.org/api/v1.0/plugin/acegi/") {
                accept "application/json"
            }

        then:"The response is a gpath result"
            mockServer.verify()
            resp != null
            resp.json instanceof JSONObject
            resp.json.name == 'acegi'
    }

    def "Test that a basic GET request returns a JSON result of the response type is JSON and accept JSON is passed"(){
        given:"A rest client instance"
            def rest = new RestBuilder()
            final mockServer = MockRestServiceServer.createServer(rest.restTemplate)
            mockServer.expect(requestTo("http://grails.org/api/v1.0/plugin/acegi/"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                    .andRespond(withSuccess('{"name":"acegi"}', MediaType.APPLICATION_JSON))


        when:"A get request is issued for a response that returns XML"
            def resp = rest.get("http://grails.org/api/v1.0/plugin/acegi/") {
                accept JSON
            }

        then:"The response is a gpath result"
            mockServer.verify()
            resp != null
            resp.json instanceof JSONObject
            resp.json.name == 'acegi'
    }

    def "Test that obtaining a 404 response doesn't throw an exception but instead returns the response object for inspection"() {
        given:"A rest client instance"
            def rest = new RestBuilder()
            final mockServer = MockRestServiceServer.createServer(rest.restTemplate)
            mockServer.expect(requestTo("http://grails.org/api/v1.0/plugin/nonsense"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header(HttpHeaders.ACCEPT, "application/xml"))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND))

        when:"A get request is issued to a URL that returns a 404"
            def resp = rest.get("http://grails.org/api/v1.0/plugin/nonsense") {
                accept "application/xml"
            }

        then:"Check the status"
            mockServer.verify()
            resp.status == 404
            resp.text instanceof String
    }

    def "Test that a basic GET request returns a JSON result of the response type is JSON with custom settings"(){
        given:"A rest client instance"
            def rest = new RestBuilder(connectTimeout:1000, readTimeout:20000)
            final mockServer = MockRestServiceServer.createServer(rest.restTemplate)
            mockServer.expect(requestTo("http://grails.org/api/v1.0/plugin/acegi/"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess('{"name":"acegi"}', MediaType.APPLICATION_JSON))


        when:"A get request is issued for a response that returns XML"
            def resp = rest.get("http://grails.org/api/v1.0/plugin/acegi/")

        then:"The response is a gpath result"
            mockServer.verify()
            resp != null
            resp.json instanceof JSONObject
            resp.json.name == 'acegi'
    }

    def "Test that a basic GET request returns a XML result of the response type is XML"(){
        given:"A rest client instance"
            def rest = new RestBuilder()
            final mockServer = MockRestServiceServer.createServer(rest.restTemplate)
            mockServer.expect(requestTo("http://grails.org/api/v1.0/plugin/acegi/"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header(HttpHeaders.ACCEPT, "application/xml"))
                    .andRespond(withSuccess('<plugin><name>acegi</name></plugin>', MediaType.APPLICATION_XML))


        when:"A get request is issued for a response that returns XML"
            def resp = rest.get("http://grails.org/api/v1.0/plugin/acegi/") {
                accept 'application/xml'
            }

        then:"The response is a gpath result"
            mockServer.verify()
            resp != null
            resp.xml instanceof GPathResult
            resp.xml.name == 'acegi'
    }

    def "Test basic authentication with GET request"() {
        given:"A rest client instance"
            def rest = new RestBuilder()
            final mockServer = MockRestServiceServer.createServer(rest.restTemplate)
            mockServer.expect(requestTo("http://repo.grails.org/grails/api/security/users"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header(HttpHeaders.AUTHORIZATION, anything()))
                    .andRespond(withSuccess('["foo", "bar"]', MediaType.APPLICATION_JSON))


        when:"A get request is issued for a response that returns XML"
            def resp = rest.get("http://repo.grails.org/grails/api/security/users"){
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
            }

        then:"The response is a gpath result"
            resp != null
            resp.json instanceof JSONArray
            mockServer.verify()
    }

    def "Test basic authentication with PUT request"() {
        given:"A rest client instance"
            def rest = new RestBuilder()
            MockRestServiceServer mockServer = mockArtifactoryUserGroupApi(rest)



        when:"A get request is issued for a response that returns XML"
            def resp = rest.put("http://repo.grails.org/grails/api/security/groups/test-group"){
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
                contentType "application/vnd.org.jfrog.artifactory.security.Group+json"
                json {
                    name = "test-group"
                    description = "A temporary test group"
                }
            }
        then:"The response is a gpath result"
            resp != null
            resp.status == 201

        when:"The resource contents are requested"
            resp = rest.get("http://repo.grails.org/grails/api/security/groups/test-group") {
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
            }

        then:"The contents are valid"
            resp != null
            resp.json.name == 'test-group'

        when:"The resource is deleted"
            resp = rest.delete("http://repo.grails.org/grails/api/security/groups/test-group") {
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
            }

        then:"The resource is gone"
            resp != null
            resp.status == 200
            mockServer.verify()
    }

    public static MockRestServiceServer mockArtifactoryUserGroupApi(RestBuilder rest) {
        final mockServer = MockRestServiceServer.createServer(rest.restTemplate)
        mockServer.expect(requestTo("http://repo.grails.org/grails/api/security/groups/test-group"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().contentType("application/vnd.org.jfrog.artifactory.security.Group+json"))
                .andExpect(content().string('{"name":"test-group","description":"A temporary test group"}'))
                .andExpect(header(HttpHeaders.AUTHORIZATION, anything()))
                .andRespond(withStatus(HttpStatus.CREATED))

        mockServer.expect(requestTo("http://repo.grails.org/grails/api/security/groups/test-group"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, anything()))
                .andRespond(withSuccess('{"name":"test-group","description":"A temporary test group"}', MediaType.valueOf("application/vnd.org.jfrog.artifactory.security.Group+json")))

        mockServer.expect(requestTo("http://repo.grails.org/grails/api/security/groups/test-group"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(HttpHeaders.AUTHORIZATION, anything()))
                .andRespond(withSuccess())
        mockServer
    }

    def "Test basic authentication with PUT request and JSON body"() {
        given:"A rest client instance"
            def rest = new RestBuilder()
            MockRestServiceServer mockServer = mockArtifactoryUserGroupApi(rest)

        when:"A get request is issued for a response that returns XML"
            def builder = new JSONBuilder()
            JSON j = builder.build {
                name = "test-group"
                description = "A temporary test group"
            }
            def resp = rest.put("http://repo.grails.org/grails/api/security/groups/test-group"){
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
                contentType "application/vnd.org.jfrog.artifactory.security.Group+json"
                body j
            }
        then:"The response is a gpath result"
            resp != null
            resp.status == 201
            resp.text == "Created"

        when:"The resource contents are requested"
            resp = rest.get("http://repo.grails.org/grails/api/security/groups/test-group") {
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
            }

        then:"The contents are valid"
            resp != null
            resp.json.name == 'test-group'

        when:"The resource is deleted"
            resp = rest.delete("http://repo.grails.org/grails/api/security/groups/test-group") {
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
            }

        then:"The resource is gone"
            resp != null
            resp.status == 200
            mockServer.verify()
    }

    def "Test basic authentication with PUT request and JSON as map"() {
        given:"A rest client instance"
            def rest = new RestBuilder()
            MockRestServiceServer mockServer = mockArtifactoryUserGroupApi(rest)

        when:"A get request is issued for a response that returns XML"
            def builder = new JSONBuilder()
            def j = [
                    name : "test-group",
                    description : "A temporary test group"
            ]
            def resp = rest.put("http://repo.grails.org/grails/api/security/groups/test-group"){
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
                contentType "application/vnd.org.jfrog.artifactory.security.Group+json"
                json j
            }
        then:"The response is a gpath result"
            resp != null
            resp.status == 201
            resp.text == "Created"

        when:"The resource contents are requested"
            resp = rest.get("http://repo.grails.org/grails/api/security/groups/test-group") {
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
            }

        then:"The contents are valid"
            resp != null
            resp.json.name == 'test-group'

        when:"The resource is deleted"
            resp = rest.delete("http://repo.grails.org/grails/api/security/groups/test-group") {
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
            }

        then:"The resource is gone"
            resp != null
            resp.status == 200
            mockServer.verify()
    }



    // Note that this test uses JSON query parameters, but they are not actually validated due to the call
    //	not using them. If a call that processes JSON URL parameters if used, this test would mean much more.
    @Issue("https://github.com/grails-plugins/grails-rest-client-builder/issues/3")
    def "Test URL variables"() {
        given:"A rest client instance"
            def rest = new RestBuilder()
            final mockServer = MockRestServiceServer.createServer(rest.restTemplate)
            mockServer.expect(requestTo("http://grails.org/api/v1.0/plugin/acegi"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header(HttpHeaders.ACCEPT, "application/json"))
                    .andRespond(withSuccess('{"name":"acegi"}', MediaType.APPLICATION_JSON))


        when:"A get request is issued for a response that returns XML"
            def resp = rest.get("http://grails.org/api/v1.0/plugin/{name}", [name: 'acegi']) {
                accept "application/json"
            }

        then:"The response is a gpath result"
            mockServer.verify()
            resp != null
            resp.json instanceof JSONObject
            resp.json.name == 'acegi'
    }
}
