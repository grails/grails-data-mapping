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

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.datastore.mapping.MappingContext;
import org.springframework.datastore.mapping.MappingFactory;
import org.springframework.datastore.mapping.MappingConfigurationStrategy;
import org.springframework.datastore.mapping.PersistentEntity;

import java.util.List;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public class GrailsApplicationMappingContext implements MappingContext {

    public GrailsApplicationMappingContext(GrailsApplication application) {
        super(); 
    }

    public List<PersistentEntity> getPersistentEntities() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PersistentEntity getPersistentEntity(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PersistentEntity addPersistentEntity(Class javaClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public MappingConfigurationStrategy getMappingSyntaxStrategy() {
        throw new UnsupportedOperationException("Method getMappingSyntaxStrategy() not supported");
    }

    public MappingFactory getMappingFactory() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
