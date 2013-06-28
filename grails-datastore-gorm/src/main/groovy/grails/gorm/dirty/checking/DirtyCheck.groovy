package grails.gorm.dirty.checking

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * @author Graeme Rocher
 */

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE])
@GroovyASTTransformationClass("org.codehaus.groovy.grails.compiler.gorm.DirtyCheckTransformation")
public @interface DirtyCheck {

}