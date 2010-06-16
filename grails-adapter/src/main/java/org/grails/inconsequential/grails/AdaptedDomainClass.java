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
package org.grails.inconsequential.grails;

import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.grails.inconsequential.mapping.ClassMapping;
import org.grails.inconsequential.mapping.PersistentEntity;
import org.grails.inconsequential.mapping.PersistentProperty;
import org.grails.inconsequential.reflect.ReflectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapts a GrailsDomainClass instance to the more generic Inconsequential API
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class AdaptedDomainClass implements PersistentEntity {
    protected GrailsDomainClass domainClass;
    protected List<PersistentProperty> persistentProperties = new ArrayList<PersistentProperty>();

    public AdaptedDomainClass(GrailsDomainClass domainClass) {
        super();
        this.domainClass = domainClass;
        GrailsDomainClassProperty[] props = domainClass.getPersistentProperties();
        for (GrailsDomainClassProperty prop : props) {
            if(prop.isAssociation()) {

            }
            else {

            }
        }
    }

    public ClassMapping getMapping() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object newInstance() {
        return ReflectionUtils.instantiate(getJavaClass());
    }

    public List<String> getPersistentPropertyNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getName() {
        return domainClass.getFullName();
    }

    public PersistentProperty getIdentity() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<PersistentProperty> getPersistentProperties() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PersistentProperty getPropertyByName(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Class getJavaClass() {
        return this.domainClass.getClazz();
    }

    public boolean isInstance(Object obj) {
        return getJavaClass().isInstance(obj);
    }

    public void initialize() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
