package org.grails.datastore.mapping.reflect

import spock.lang.Specification
import spock.lang.Unroll

class NameUtilsSpec extends Specification {

    @Unroll
    void "decapitalizeFirstChar for #name should be #expected"(String name, String expected) {
        expect:
        NameUtils.decapitalizeFirstChar(name) == expected

        where:
        name    | expected
        'Name'  | 'name'
        'name'  | 'name'
        'IName' | 'iName'
    }
}
