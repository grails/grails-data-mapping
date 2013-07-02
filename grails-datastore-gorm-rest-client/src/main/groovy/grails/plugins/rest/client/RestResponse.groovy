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
package grails.plugins.rest.client

import grails.converters.JSON
import grails.converters.XML
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult

import org.codehaus.groovy.grails.web.json.JSONElement
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpStatusCodeException

/**
 *
 * Wraps a {@link ResponseEntity} allowing easy access to the underlying JSON or XML response. All methods of {@link ResponseEntity} are available on this class
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class RestResponse {

    @Delegate ResponseEntity responseEntity

    RestResponse(ResponseEntity responseEntity) {
        this.responseEntity = responseEntity
    }

    @Lazy JSONElement json = {
        def body = responseEntity.body
        if (body) {
            return (JSONElement)JSON.parse(body.toString())
        }
    }()

    @Lazy GPathResult xml = {
        def body = responseEntity.body
        if (body) {
            return (GPathResult)XML.parse(body.toString())
        }
    }()

    @Lazy String text = {
        def body = responseEntity.body
        if (body) {
            return body.toString()
        }
        else {
            return responseEntity.statusCode.reasonPhrase
        }
    }()

    int getStatus() {
        responseEntity?.statusCode?.value() ?: 200
    }


}
