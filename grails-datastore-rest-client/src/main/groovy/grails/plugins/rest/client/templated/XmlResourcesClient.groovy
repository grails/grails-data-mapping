/* Copyright (C) 2013 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugins.rest.client.templated

import grails.converters.XML
import grails.plugins.rest.client.RestBuilder
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult
import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import org.grails.web.converters.ConverterUtil

/**
 *
 * A simple client for interacting with a REST web service exposed by a Grails application using XML as an interchange format
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class XmlResourcesClient extends AbstractResourcesClient<GPathResult>{
    /**
     * @param url The base URL. Example http://localhost:8080/books
     */
    XmlResourcesClient(String url) {
        super(url)
    }

    XmlResourcesClient(String url, RestBuilder restBuilder) {
        super(url, restBuilder)
    }

    @Override
    protected Object convertBody(Object requestBody) {
        if( (requestBody instanceof XML) || (requestBody instanceof GPathResult) ) {
            return requestBody
        }
        try {
            return requestBody as XML
        } catch (GroovyCastException e) {
            return ConverterUtil.createConverter(XML, requestBody)
        }
    }

    @Override
    Class getAcceptType() { XML }

    @Override
    String getAcceptContentType() { "application/xml" }
}
