package org.grails.datastore.mapping.reflect

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.GenericsType
import org.codehaus.groovy.ast.tools.GenericsUtils

/**
 * Generics utilities
 */
@CompileStatic
class AstGenericsUtils extends GenericsUtils {

    /**
     * Resolves a single generic type from the given class node
     *
     * @param classNode The class node
     * @return The generic type
     */
    static ClassNode resolveSingleGenericType(ClassNode classNode) {
        if(classNode.isArray()) {
            return classNode.componentType.plainNodeReference
        }
        GenericsType[] genericsTypes = classNode.genericsTypes
        if(genericsTypes) {
            return genericsTypes[0].type.plainNodeReference
        }
        else {
            return ClassHelper.OBJECT_TYPE
        }
    }
}
