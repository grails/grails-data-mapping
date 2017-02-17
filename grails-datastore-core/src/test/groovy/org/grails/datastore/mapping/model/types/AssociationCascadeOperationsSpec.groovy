package org.grails.datastore.mapping.model.types

import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.ClassMapping
import org.grails.datastore.mapping.model.PropertyMapping
import spock.lang.Specification
import javax.persistence.CascadeType

class AssociationCascadeOperationsSpec extends Specification {

    void "test getCascadeOperations"() {
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
        List<CascadeType> cascadeTypes = a.getCascadeOperations()

        then:
        cascadeTypes == expectedTypes

        where:
        _cascade   | _owningSide | expectedTypes
        "none"     | false       | []
        "all"      | false       | [CascadeType.ALL]
        "merge"    | false       | [CascadeType.MERGE]
        "delete"   | false       | [CascadeType.REMOVE]
        "remove"   | false       | [CascadeType.REMOVE]
        "refresh"  | false       | [CascadeType.REFRESH]
        "persist"  | false       | [CascadeType.PERSIST]
        "abc"      | false       | []
        "none"     | true        | []
        "all"      | true        | [CascadeType.ALL]
        "merge"    | true        | [CascadeType.MERGE]
        "delete"   | true        | [CascadeType.REMOVE]
        "remove"   | true        | [CascadeType.REMOVE]
        "refresh"  | true        | [CascadeType.REFRESH]
        "persist"  | true        | [CascadeType.PERSIST]
        "abc"      | true        | []
        null       | true        | [CascadeType.ALL]
        null       | false       | [CascadeType.PERSIST]
        "delete,merge"  | false  | [CascadeType.REMOVE,CascadeType.MERGE]
        "delete, merge" | false  | [CascadeType.REMOVE,CascadeType.MERGE]

    }
}