package org.grails.orm.hibernate.cfg

class SortConfig {
    String name
    String direction
    Map namesAndDirections

    Map getNamesAndDirections() {
        if (namesAndDirections) {
            return namesAndDirections
        }
        if (name) {
            return [(name): direction]
        }
        Collections.emptyMap()
    }
}
