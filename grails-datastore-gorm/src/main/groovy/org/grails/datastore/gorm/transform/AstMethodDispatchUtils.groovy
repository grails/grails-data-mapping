package org.grails.datastore.gorm.transform

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.transform.sc.transformers.ConstructorCallTransformer
import org.codehaus.groovy.transform.stc.StaticTypesMarker

import static org.grails.datastore.mapping.reflect.AstUtils.*

/**
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class AstMethodDispatchUtils extends GeneralUtils {

    /**
     * Builds a direct dispatch call to the given constructor for the given arguments
     * @param type
     * @param args
     * @return
     */
    public static Expression ctorD(ClassNode type, Expression args = ZERO_ARGUMENTS) {
        ConstructorCallExpression cce = ctorX( type, args )
        ConstructorNode cn = type.getDeclaredConstructor()
        if(cn != null) {
            cce.putNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET, cn)
        }
        return cce
    }
}
