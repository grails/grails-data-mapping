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

    /**
     * @return The domain class this service operates with
     */
    Class value() default Object.class;

    /**
     * Whether to make the service available to GORM. Defaults to true, which will generate the appropriate META-INF/services file at compile time.
     *
     * @return True if it should be made available
     */
    boolean expose() default true;
}
