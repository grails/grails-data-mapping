package grails.gorm.services;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes any class into a GORM {@link org.grails.datastore.mapping.services.Service}
 *
 * @since 6.1
 * @author Graeme Rocher
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass("org.grails.datastore.gorm.services.ServiceTransformation")
public @interface Service {
}
