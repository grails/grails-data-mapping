package grails.gorm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

/**
 * AST transformation for transforming a GORM entity into a Spring Data Cassandra entity
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass("org.grails.datastore.gorm.cassandra.GormToCassandraTransform")
public @interface CassandraEntity {
}
