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

/**
 * @author Graeme Rocher
 */
class AsyncRestBuilderSpec extends Specification{
    void setup() {
        def ga = new DefaultGrailsApplication()
        new ConvertersConfigurationInitializer().initialize(ga)
    }

    void cleanup() {
        ConvertersConfigurationHolder.clear()
    }

    def "Test that a basic GET request returns a JSON result of the response type is JSON"(){
        given:"A rest client instance"
            def rest = new AsyncRestBuilder()

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
        when:"A get request is issued to a URL that returns a 404"
            Promise<RestResponse> resp = rest.get("http://grails.org/api/v1.0/plugin/nonsense") {
                accept "application/xml"
            }

        then:"Check the status"
            resp.get().status == 404
            resp.get().text instanceof String
            resp.get().body instanceof byte[]
    }

    def "Test that a basic GET request returns a JSON result of the response type is JSON with custom settings"(){
        given:"A rest client instance"
            def rest = new AsyncRestBuilder(connectTimeout:1000, readTimeout:20000)

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
            resp.get().text == "Group 'test-group' has been removed successfully."
    }

    def "Test basic authentication with PUT request and JSON body"() {
        given:"A rest client instance"
            def rest = new AsyncRestBuilder()

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
            resp.get().text == "Group 'test-group' has been removed successfully."
    }

    def "Test basic authentication with PUT request and JSON as map"() {
        given:"A rest client instance"
            def rest = new AsyncRestBuilder()

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
            resp.get().text == "Group 'test-group' has been removed successfully."
    }

    def "Test PUT request passing binary content in the body"() {
        setup:
            def rest = new AsyncRestBuilder(connectTimeout: 1000, readTimeout:10000)
            rest.delete("http://repo.grails.org/grails/libs-snapshots-local/org/mycompany/1.0/foo-1.0.jar") {
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
            }.get()


        when:"A put request is issued that contains binary content"
            Promise<RestResponse> resp = rest.put("http://repo.grails.org/grails/libs-snapshots-local/org/mycompany/1.0/foo-1.0.jar") {
                auth System.getProperty("artifactory.user"), System.getProperty("artifactory.pass")
                body "foo".bytes
            }

        then:"The response JSON is correct"
            resp.get().json.uri == "http://repo.grails.org/grails/libs-snapshots-local/org/mycompany/1.0/foo-1.0.jar"
            new URL( "http://repo.grails.org/grails/libs-snapshots-local/org/mycompany/1.0/foo-1.0.jar").text == "foo"
    }

    // Note that this test uses JSON query parameters, but they are not actually validated due to the call
    //	not using them. If a call that processes JSON URL parameters if used, this test would mean much more.
    @Issue("https://github.com/grails-plugins/grails-rest-client-builder/issues/3")
    def "Test URL variables for JSON URL paremeters"() {
        given:"A rest client instance"
            def rest = new AsyncRestBuilder()

        when:"A get request with URL parameters defined as an implicit map"
            Promise<RestResponse> resp = rest.get("http://grails.org/api/v1.0/plugin/acegi/?query={query}&filter={filter}") {
                urlVariables query:'{"query":true}', filter:'{"filter":true}'
            }

        then:"The response is a gpath result"
            resp != null
            resp.get().json instanceof JSONObject
            resp.get().json.name == 'acegi'

        when:"A get request with URL parameters defined as an explicit map"
            resp = rest.get("http://grails.org/api/v1.0/plugin/acegi/?query={query}&filter={filter}") {
                urlVariables([query:'{"query":true}', filter:'{"filter":true}'])
            }

        then:"The response is a gpath result"
            resp != null
            resp.get().json instanceof JSONObject
            resp.get().json.name == 'acegi'
    }
}
