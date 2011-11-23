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
package org.grails.datastore.gorm.neo4j.constraints

import org.codehaus.groovy.grails.validation.AbstractConstraint
import org.grails.datastore.gorm.neo4j.GrailsRelationshipTypes
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.validation.Errors

/**
 * The UniqueConstraint depends on the current GORM implementation, that's why we need to implement it here.
 * @author Stefan Armbruster
 * @since 0.2
 */
class UniqueConstraint extends AbstractConstraint {

    protected final Logger log = LoggerFactory.getLogger(getClass())
    protected static final String DEFAULT_NOT_UNIQUE_MESSAGE_CODE = "default.not.unique.message"

    public static final String UNIQUE_CONSTRAINT = "unique"

    protected void processValidate(target, propertyValue, Errors errors) {

        if (!constraintParameter) {
            return
        }

        Node subreferenceNode = target.subreferenceNode
        log.debug "subreferenceNode $subreferenceNode"
        // TODO: consider using index for this

        Relationship hasMatchingNode = subreferenceNode.getRelationships(GrailsRelationshipTypes.INSTANCE, Direction.OUTGOING).iterator().find {
            (it.endNode.id != target.id) && (it.endNode.getProperty(constraintPropertyName, null) == propertyValue)

        }
        log.debug "hasMatchingNode $hasMatchingNode"

        if (hasMatchingNode) {
            rejectValue(target, errors, UNIQUE_CONSTRAINT,
                    [constraintPropertyName, constraintOwningClass, propertyValue ],
                    getDefaultMessage(DEFAULT_NOT_UNIQUE_MESSAGE_CODE))
        }
    }

    boolean supports(Class type) {
        type != null
    }

    String getName() {
        UNIQUE_CONSTRAINT
    }
}
