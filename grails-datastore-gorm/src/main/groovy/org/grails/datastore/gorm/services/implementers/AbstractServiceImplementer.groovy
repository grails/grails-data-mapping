package org.grails.datastore.gorm.services.implementers

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.transform.trait.Traits
import org.grails.datastore.gorm.services.ServiceImplementer
import org.grails.datastore.gorm.transform.AstPropertyResolveUtils
import org.grails.datastore.mapping.core.Ordered
import org.grails.datastore.mapping.model.config.GormProperties
import org.grails.datastore.mapping.reflect.AstUtils

/**
 * Abstract implementation of the {@link ServiceImplementer} interface
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
abstract class AbstractServiceImplementer implements PrefixedServiceImplementer, Ordered {

    @Override
    boolean doesImplement(ClassNode domainClass, MethodNode methodNode) {
        def alreadyImplemented = methodNode.getNodeMetaData(IMPLEMENTED)

        String prefix = resolvePrefix(methodNode)
        if(!alreadyImplemented && prefix) {
            ClassNode returnType = methodNode.returnType
            return isCompatibleReturnType(domainClass, methodNode, returnType, prefix)
        }
        return false
    }

    @Override
    String resolvePrefix(MethodNode mn) {
        return handledPrefixes.find() { String it -> mn.name.startsWith(it) }
    }
    /**
     * Return true if the provided return type is compatible with this implementor.
     *
     * @param domainClass The domain class this method applies to
     * @param methodNode The method
     * @param returnType The return type
     * @return True if it is a compatible return type
     */
    protected abstract boolean isCompatibleReturnType(ClassNode domainClass, MethodNode methodNode, ClassNode returnType, String prefix)


    /**
     * Copies annotation from the abstract method to the implementation method
     *
     * @param abstractMethod the abstract method
     * @param impl the implementation method
     */
    protected void copyClassAnnotations(final MethodNode abstractMethod, final MethodNode impl) {
        List<AnnotationNode> annotations = abstractMethod.getAnnotations()
        for (AnnotationNode annotation in annotations) {
            if (annotation.getClassNode() != Traits.TRAIT_CLASSNODE) {
                impl.addAnnotation(annotation)
            }
        }
    }

    /**
     * Check whether the given parameter is a valid property of the domain class
     *
     * @param domainClassNode The domain class
     * @param parameter The parameter
     * @param parameterName The parameter name
     * @return True if it is
     */
    protected boolean isValidParameter(ClassNode domainClassNode, Parameter parameter, String parameterName) {
        if(GormProperties.IDENTITY.equals(parameterName)) {
            return true
        }
        else {
            ClassNode propertyType = AstPropertyResolveUtils.getPropertyType(domainClassNode, parameterName)
            if(propertyType != null && (propertyType == parameter.type || AstUtils.isSubclassOf(parameter.type, propertyType.name))) {
                return true
            }
        }
        return false
    }
}
