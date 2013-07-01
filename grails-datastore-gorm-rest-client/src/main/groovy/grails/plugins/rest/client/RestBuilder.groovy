package grails.plugins.rest.client

import static org.springframework.http.MediaType.*

import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate

class RestBuilder {

    RestTemplate restTemplate = new RestTemplate()

    RestBuilder() {}

    RestBuilder(Map settings) {

        def proxyHost = System.getProperty("http.proxyHost")
        def proxyPort = System.getProperty("http.proxyPort")

        if (proxyHost && proxyPort) {
            if (settings.proxy == null) {
                settings.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort.toInteger()))
            }
        }

        if (settings.proxy instanceof Map) {
            def ps = settings.proxy.entrySet().iterator().next()
            if (ps.value) {
                def proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ps.key, ps.value.toInteger()))
                settings.proxy = proxy
            }
        }

        restTemplate.setRequestFactory(new SimpleClientHttpRequestFactory(settings))
    }

    /**
     * Issues a GET request and returns the response in the most appropriate type
     * @param url The URL
     * @param url The closure customizer used to customize request attributes
     */
    def get(String url, Closure customizer = null) {
        doRequestInternal(url, customizer, HttpMethod.GET)
    }

    /**
     * Issues a PUT request and returns the response in the most appropriate type
     *
     * @param url The URL
     * @param customizer The clouser customizer
     */
    def put(String url, Closure customizer = null) {
        doRequestInternal(url, customizer, HttpMethod.PUT)
    }

    /**
     * Issues a POST request and returns the response
     * @param url The URL
     * @param customizer (optional) The closure customizer
     */
    def post(String url, Closure customizer = null) {
        doRequestInternal(url, customizer, HttpMethod.POST)
    }

    /**
     * Issues DELETE a request and returns the response

     * @param url The URL
     * @param customizer (optional) The closure customizer
     */
    def delete(String url, Closure customizer = null) {
        doRequestInternal(url, customizer, HttpMethod.DELETE)
    }

    protected doRequestInternal(String url, Closure customizer, HttpMethod method) {

        def requestCustomizer = new RequestCustomizer()
        if (customizer != null) {
            customizer.delegate = requestCustomizer
            customizer.call()
        }

        try {
            def responseEntity = restTemplate.exchange(url, method, requestCustomizer.createEntity(),
                    String, requestCustomizer.getVariables())
            handleResponse(responseEntity)
        }
        catch (HttpStatusCodeException e) {
            return new ErrorResponse(error:e)
        }
    }

    protected handleResponse(ResponseEntity responseEntity) {
        return new RestResponse(responseEntity: responseEntity)
    }
}
