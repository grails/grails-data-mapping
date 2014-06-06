package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.model.types.ManyToMany;
import org.grails.datastore.mapping.model.types.OneToMany;

/**
 * Created by stefan on 03.03.14.
 */
public abstract class RelationshipUtils {

    public static boolean useReversedMappingFor(Association association) {
        return association.isBidirectional() &&
                ((association instanceof OneToMany) ||
                        ((association instanceof ManyToMany) && (association.isOwningSide())));
    }

    public static String relationshipTypeUsedFor(Association association) {
        String name = useReversedMappingFor(association) ?
                association.getReferencedPropertyName() :
                association.getName();
        return name.toUpperCase();
    }
}
