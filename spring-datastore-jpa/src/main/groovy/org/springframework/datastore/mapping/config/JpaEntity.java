package org.springframework.datastore.mapping.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

/**
 * AST transformation for transforming a GORM entity into a JPA entity
 * 
 * @author Graeme Rocher
 * @since 1.0
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass("org.springframework.datastore.mapping.config.GormToJpaTransform")
public @interface JpaEntity {

}
