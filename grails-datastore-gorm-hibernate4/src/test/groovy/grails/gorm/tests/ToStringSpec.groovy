package grails.gorm.tests

import org.codehaus.groovy.ast.ClassNode
import org.grails.compiler.injection.GrailsASTUtils
import spock.lang.Specification

/**
 * Created by graemerocher on 05/04/16.
 */
class ToStringSpec extends Specification {

    void "Test overridden toString() method"() {
        expect:
        new Club(name: "Manchester United").toString() == 'Manchester United'
    }

    void "Test toString() annotation"() {
        expect:
        GrailsASTUtils.hasAnnotation(new ClassNode(Team), groovy.transform.ToString.class)
        new Team(name: "Manchester United First Team").toString() == "grails.gorm.tests.Team(Manchester United First Team)"

    }
}
