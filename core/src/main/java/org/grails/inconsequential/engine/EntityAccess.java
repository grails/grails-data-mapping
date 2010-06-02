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
package org.grails.inconsequential.engine;

import org.apache.commons.beanutils.BeanMap;

/**
 * Class used to access properties of an entity. Also responsible for
 * any conversion from source to target types.
 * 
 * @author Graeme Rocher
 * @since 1.0
 */
public class EntityAccess {

    protected Object entity;
    protected BeanMap beanMap;

    public EntityAccess(Object entity) {
        this.entity = entity;
        this.beanMap = new BeanMap(entity);
    }

    public Object getEntity() {
        return entity;
    }

    public Object getProperty(String name) {
        return beanMap.get(name);
    }

    public Object setProperty(String name, Object value) {
        return beanMap.put(name, value);
    }
}
