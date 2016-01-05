/*
 * Copyright 2015 original authors
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

package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.grails.datastore.gorm.neo4j.mapping.config.DynamicAssociation
import org.grails.datastore.gorm.neo4j.mapping.config.DynamicToOneAssociation
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ManyToMany
import org.grails.datastore.mapping.model.types.OneToMany

/**
 * Utility methods for manipulating relationships
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@CompileStatic
class RelationshipUtils {

    @Memoized
    public static boolean useReversedMappingFor(Association association) {
        return association.isBidirectional() &&
                ((association instanceof OneToMany) ||
                        ((association instanceof ManyToMany) && (association.isOwningSide())));
    }

    @Memoized
    public static String relationshipTypeUsedFor(Association association) {
        String name = useReversedMappingFor(association) ?
                association.getReferencedPropertyName() :
                association.getName()

        if(association instanceof DynamicAssociation) {
            return name
        }
        else {
            return name.toUpperCase()
        }
    }
}
