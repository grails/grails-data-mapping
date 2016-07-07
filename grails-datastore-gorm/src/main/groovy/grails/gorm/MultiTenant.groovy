package grails.gorm

import groovy.transform.CompileStatic

/**
 * A trait for domain classes to implement that should be treated as multi tenant
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
trait MultiTenant extends Entity {

}