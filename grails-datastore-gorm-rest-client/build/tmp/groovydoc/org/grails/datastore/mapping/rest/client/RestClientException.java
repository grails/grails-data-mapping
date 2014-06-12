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
package org.grails.datastore.mapping.rest.client;

import org.grails.datastore.mapping.core.DatastoreException;
import grails.plugins.rest.client.RestResponse;

/**
 * Exception thrown from requests made by GORM for REST
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RestClientException extends DatastoreException {
    private RestResponse response;

    public RestClientException(String s, RestResponse response) {
        super(s);
        this.response = response;
    }

    public RestClientException(String s, Throwable throwable, RestResponse response) {
        super(s, throwable);
        this.response = response;
    }
    
    public RestResponse getResponse() { 
        return this.response;
    }
}
