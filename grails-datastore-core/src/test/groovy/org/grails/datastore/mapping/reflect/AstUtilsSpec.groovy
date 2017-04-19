package org.grails.datastore.mapping.reflect

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import spock.lang.Specification

/**
 * Created by graemerocher on 19/04/2017.
 */
class AstUtilsSpec extends Specification {

    void "test implements interface"() {
        given:
        ClassNode node = ClassHelper.make("Test")
        def itfc = ClassHelper.make("ITest")
        node.addInterface(itfc)

        expect:
        AstUtils.implementsInterface(node, itfc)
        AstUtils.implementsInterface(node, itfc.name)
        !AstUtils.implementsInterface(node, "Another")
    }
}
