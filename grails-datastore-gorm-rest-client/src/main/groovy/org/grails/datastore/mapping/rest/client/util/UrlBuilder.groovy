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
package org.grails.datastore.mapping.rest.client.util

import groovy.transform.CompileStatic

/**
 * Simply utility class for building URLs
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class UrlBuilder {

    StringBuilder url
    private boolean first = true
    private boolean hasPrevious = false

    UrlBuilder(String baseUrl) {
        url = new StringBuilder(baseUrl)
    }

    void addParam(String name, value) {
        if(first) {
            url << '?'
            first = false
        }

        if(hasPrevious) {
            url << '&'
        }
        else {
            hasPrevious = true
        }
        url << name << '=' << value
    }

    @Override
    String toString() {
        url.toString()
    }
}
