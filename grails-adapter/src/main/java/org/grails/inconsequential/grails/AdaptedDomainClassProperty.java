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

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.grails.inconsequential.mapping.ClassMapping;
import org.grails.inconsequential.mapping.PersistentProperty;
import org.grails.inconsequential.mapping.PropertyMapping;

/**
 * Adapts the GrailsDomainClassProperty interface to the Inconsequential
 * equivalents
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class AdaptedDomainClassProperty implements PersistentProperty {
    protected GrailsDomainClassProperty property;

    public AdaptedDomainClassProperty(GrailsDomainClassProperty prop) {
        super();
        this.property = prop;
    }

    public String getName() {
        return property.getName();
    }

    public Class getType() {
        return property.getType();
    }

    public PropertyMapping getMapping() {
        return new PropertyMapping() {

            public ClassMapping getClassMapping() {
                return null;
            }

            public Object getMappedForm() {
                return null; 
            }
        };
    }
}
