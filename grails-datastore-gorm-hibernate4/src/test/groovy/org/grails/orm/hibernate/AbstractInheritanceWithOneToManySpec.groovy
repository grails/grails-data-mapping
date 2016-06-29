package org.grails.orm.hibernate

import grails.persistence.Entity

/*
 * Copyright 2014 original authors
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

/**
 * @author graemerocher
 */
class AbstractInheritanceWithOneToManySpec extends GormSpec {

    void "Test deleting nodes From a hierarchy"() {
        given:
        // Create the various nodes
        def theParent = new IllustrationCase(
                name: "Parent Node"
        )
        def theChild1 = new IllustrationCase(
                name: "Child Node 1"
        )
        def theChild2 = new IllustrationCase(
                name: "Child Node 2"
        )
        def theChild3 = new IllustrationCase(
                name: "Child Node 3"
        )

        // Create the hierarchy structure
        theParent.addToChildren(
                theChild1
        ).addToChildren(
                theChild2
        ).addToChildren(
                theChild3
        )

        // Perform a DEEP save
        theParent.save( failOnError: true )

        // Remove a child node --- This is where the error occurs...
        IllustrationCase.get( theParent.id ).removeFromChildren( IllustrationCase.get( theChild2.id ) )

        expect:

        // Find the parent nodes...
        def targetNode = IllustrationCase.findByName( "Parent Node" )

        targetNode.children.size() == 2
        !targetNode.children.find { it.name == "Child Node 2" }

        targetNode.children.find { it.name == "Child Node 1" }
        targetNode.children.find { it.name == "Child Node 3" }
    }

    @Override
    List getDomainClasses() {
        [AbstractBaseClass, IllustrationCase]
    }
}

// CLASS 1 - Abstract Base Class

@Entity
abstract class AbstractBaseClass {

    static hasMany = [ children: AbstractBaseClass ]

    static belongsTo = [ parent: AbstractBaseClass ]


    static constraints = {
    }
}

// CLASS 2 - Instantiable Sub Class

@Entity
class IllustrationCase extends AbstractBaseClass {

    String name

    static constraints = {
    }
}