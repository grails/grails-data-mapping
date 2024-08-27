package org.grails.datastore.mapping.model.types

import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.ClassMapping
import org.grails.datastore.mapping.model.PropertyMapping
import spock.lang.Specification
import spock.lang.Unroll

import jakarta.persistence.CascadeType

class AssociationCascadeOperationsSpec extends Specification {

    @Unroll
    void "test getCascadeOperations with value #_cascade"() {
        String cascade = _cascade
        boolean owningSide = _owningSide
        Association a = new Association<Property>(null, null, null, null) {
            @Override
            PropertyMapping getMapping() {
                new PropertyMapping() {
                    @Override
                    ClassMapping getClassMapping() {
                        return null
                    }

                    @Override
                    Property getMappedForm() {
                        Property p = new Property()
                        p.cascade = cascade
                        return p
                    }
                }
            }

            @Override
            boolean isOwningSide() {
                return owningSide
            }
        }

        when:
        Set<CascadeType> cascadeTypes = a.getCascadeOperations()

        then:
        cascadeTypes == expectedTypes

        where:
        _cascade   | _owningSide | expectedTypes
        "none"     | false       | Collections.emptySet()
        "all"      | false       | [CascadeType.ALL] as Set
        "merge"    | false       | [CascadeType.MERGE] as Set
        "delete"   | false       | [CascadeType.REMOVE] as Set
        "remove"   | false       | [CascadeType.REMOVE] as Set
        "refresh"  | false       | [CascadeType.REFRESH] as Set
        "persist"  | false       | [CascadeType.PERSIST] as Set
        "abc"      | false       | Collections.emptySet()
        "none"     | true        | Collections.emptySet()
        "all"      | true        | [CascadeType.ALL] as Set
        "merge"    | true        | [CascadeType.MERGE] as Set
        "delete"   | true        | [CascadeType.REMOVE] as Set
        "remove"   | true        | [CascadeType.REMOVE] as Set
        "refresh"  | true        | [CascadeType.REFRESH] as Set
        "persist"  | true        | [CascadeType.PERSIST] as Set
        "abc"      | true        | Collections.emptySet()
        null       | true        | [CascadeType.ALL] as Set
        null       | false       | [CascadeType.PERSIST] as Set
        "delete,merge"  | false  | [CascadeType.REMOVE,CascadeType.MERGE] as Set
        "delete, merge" | false  | [CascadeType.REMOVE,CascadeType.MERGE] as Set

    }
}