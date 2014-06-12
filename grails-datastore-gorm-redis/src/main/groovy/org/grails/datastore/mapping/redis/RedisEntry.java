/* Copyright (C) 2010 SpringSource
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
package org.grails.datastore.mapping.redis;

import java.util.HashMap;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class RedisEntry extends HashMap {

    private static final long serialVersionUID = 1;

    protected String family;

    public RedisEntry(String family) {
        this.family = family;
    }

    public String getFamily() {
        return family;
    }

    @Override
    public Object get(Object o) {

        final Object val = super.get(o);
        if (val != null) {
            return val;
        }
        return null;
    }
}
