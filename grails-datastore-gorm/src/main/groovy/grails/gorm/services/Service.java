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
// TODO: RxJava,Promises, Where queries, interface projections
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass("org.grails.datastore.gorm.services.transform.ServiceTransformation")
public @interface Service {

    /**
     * @return The domain class this service operates with
     */
    Class value() default Object.class;

    /**
     * @return The name of the service, by default this will the class name decapitalized. ie. BookService = bookService
     */
    String name() default "";
}
