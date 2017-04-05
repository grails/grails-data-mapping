package org.grails.datastore.gorm.services.implementers

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode

/**
 * For projections that return an iterable
 *
 * @author Graeme Rocher
 * @since 6.1.1
 */
interface IterableProjectionServiceImplementer extends IterableServiceImplementer  {
    /**
     * Is the return type compatible with the projection query
     *
     * @param domainClass The domain class
     * @param methodNode the method noe
     * @param prefix The resolved prefix
     * @return True if it is
     */
    boolean isCompatibleReturnType(ClassNode domainClass, MethodNode methodNode, ClassNode returnType, String prefix)

}
