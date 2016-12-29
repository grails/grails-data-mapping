package grails.gorm.annotation.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for providing additional attributes about an entity
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface EntityAttributes {
    /**
     * @return Whether the entity is validateable via javax.validation
     */
    boolean validateable() default false;
}
