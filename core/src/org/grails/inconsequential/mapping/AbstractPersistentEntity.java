/* Copyright 2004-2005 the original author or authors.
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
package org.grails.inconsequential.mapping;

import java.util.List;

/**
 * Abstract implementation to be subclasses on a per datastore basis
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class AbstractPersistentEntity implements PersistentEntity{
    protected Class javaClass;

    public AbstractPersistentEntity(Class javaClass) {
        if(javaClass == null) throw new IllegalArgumentException("The argument [javaClass] cannot be null");
        this.javaClass = javaClass;
    }

    public String getName() {
        return javaClass.getName();
    }

    public List<PersistentProperty> getPersistentProperties() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PersistentProperty getPropertyByName(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
