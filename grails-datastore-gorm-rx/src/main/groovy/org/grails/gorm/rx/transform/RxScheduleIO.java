package org.grails.gorm.rx.transform;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;
import org.grails.datastore.gorm.transform.GormASTTransformationClass;

import java.lang.annotation.*;

/**
 * A transformation that transforms the body of a method to return an Observable that runs on the IO Scheduler
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Inherited
@Documented
@GroovyASTTransformationClass("org.grails.datastore.gorm.transform.OrderedGormTransformation")
@GormASTTransformationClass("org.grails.gorm.rx.transform.RxScheduleIOTransformation")
public @interface RxScheduleIO {
    boolean singleResult() default false;
}
