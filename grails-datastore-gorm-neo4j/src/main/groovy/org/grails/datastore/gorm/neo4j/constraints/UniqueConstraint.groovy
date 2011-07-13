package org.grails.datastore.gorm.neo4j.constraints

import org.codehaus.groovy.grails.validation.AbstractConstraint
import org.grails.datastore.gorm.neo4j.GrailsRelationshipTypes
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.ReturnableEvaluator
import org.neo4j.graphdb.TraversalPosition
import org.neo4j.graphdb.Traverser
import org.neo4j.graphdb.Node
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.validation.Errors

/**
 *
 * The UniqueConstraint depends on the current GORM implementation, that's why we need to implement it here.
 * @author Stefan Armbruster
 * @since 0.2
 */

public class UniqueConstraint extends AbstractConstraint {

    private static final Logger log = LoggerFactory.getLogger(UniqueConstraint.class)

    public static final String UNIQUE_CONSTRAINT = "unique";
    private static final String DEFAULT_NOT_UNIQUE_MESSAGE_CODE = "default.not.unique.message";

    protected void processValidate(Object target, Object propertyValue, Errors errors) {

        if (constraintParameter) {

            Node subreferenceNode = target.subreferenceNode
            log.debug "subreferenceNode $subreferenceNode"
            // TODO: consider using index for this

            def hasMatchingNode = subreferenceNode.getRelationships(GrailsRelationshipTypes.INSTANCE, Direction.OUTGOING).find {
                it.endNode.getProperty(constraintPropertyName, null)==propertyValue
            }
            log.debug "hasMatchingNode $hasMatchingNode"

            if (hasMatchingNode) {
                rejectValue(target, errors, UNIQUE_CONSTRAINT, [constraintPropertyName, constraintOwningClass, propertyValue ], getDefaultMessage(DEFAULT_NOT_UNIQUE_MESSAGE_CODE));
            }
        }
    }

    public boolean supports(Class type) {
        return type != null;
    }

    public String getName() {
        UNIQUE_CONSTRAINT
    }

}
