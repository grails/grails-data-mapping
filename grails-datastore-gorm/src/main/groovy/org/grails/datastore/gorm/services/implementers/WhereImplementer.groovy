package org.grails.datastore.gorm.services.implementers

import grails.gorm.DetachedCriteria
import grails.gorm.services.Where
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.CodeVisitorSupport
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.Variable
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.control.SourceUnit
import org.grails.datastore.gorm.query.transform.DetachedCriteriaTransformer
import org.grails.datastore.mapping.core.Ordered
import org.grails.datastore.mapping.reflect.AstUtils

import static org.codehaus.groovy.ast.tools.GeneralUtils.args
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignS
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX
import static org.codehaus.groovy.ast.tools.GeneralUtils.classX
import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorX
import static org.codehaus.groovy.ast.tools.GeneralUtils.declS
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX
import static org.grails.datastore.mapping.reflect.AstUtils.processVariableScopes

/**
 * Created by graemerocher on 09/02/2017.
 */
@CompileStatic
class WhereImplementer extends AbstractReadOperationImplementer {


    public static final ClassNode WHERE_ANNOTATION = new ClassNode(Where)

    @Override
    int getOrder() {
        return FindByImplementer.POSITION - 100
    }

    @Override
    boolean doesImplement(ClassNode domainClass, MethodNode methodNode) {
        if( AstUtils.findAnnotation(methodNode, Where) != null) {
            return isCompatibleReturnType(domainClass, methodNode, methodNode.returnType, methodNode.name)
        }
        return false
    }

    @Override
    void doImplement(ClassNode domainClassNode, MethodNode abstractMethodNode, MethodNode newMethodNode, ClassNode targetClassNode) {

        AnnotationNode annotationNode = AstUtils.findAnnotation(abstractMethodNode, Where)
        abstractMethodNode.annotations.remove(annotationNode)
        Expression expr = annotationNode.getMember("value")
        SourceUnit sourceUnit = abstractMethodNode.declaringClass.module.context
        if(expr instanceof ClosureExpression) {
            ClosureExpression originalClosureExpression = (ClosureExpression) expr
            // make a copy
            ClosureExpression closureExpression = new ClosureExpression(originalClosureExpression.parameters, originalClosureExpression.code)
            originalClosureExpression.setCode(new BlockStatement())
            VariableScope scope = newMethodNode.variableScope
            closureExpression.setVariableScope(scope)
            if(scope != null) {
                for(Parameter p in newMethodNode.parameters) {
                    p.setClosureSharedVariable(true)
                    scope.putReferencedLocalVariable(p)
                    scope.putDeclaredVariable(p)
                }
            }
            DetachedCriteriaTransformer transformer = new DetachedCriteriaTransformer(sourceUnit)
            CodeVisitorSupport variableTransformer = new ClassCodeExpressionTransformer() {
                @Override
                protected SourceUnit getSourceUnit() {
                    sourceUnit
                }

                @Override
                Expression transform(Expression exp) {
                    if(exp instanceof VariableExpression) {
                        VariableExpression var = (VariableExpression)exp
                        def local = scope.getReferencedLocalVariable(var.name)
                        if(local != null) {
                            def newExpr = new VariableExpression(local)
                            newExpr.setClosureSharedVariable(true)
                            newExpr.setAccessedVariable(local)
                            return newExpr
                        }
                    }
                    return super.transform(exp)
                }
            }
            variableTransformer.visitClosureExpression(closureExpression)
            transformer.transformClosureExpression( newMethodNode.returnType, closureExpression)

            BlockStatement body = (BlockStatement)newMethodNode.getCode()

            Expression argsExpression = null
            for(parameter in newMethodNode.parameters) {
                if(parameter.name == 'args' && parameter.type == ClassHelper.MAP_TYPE) {
                    argsExpression = varX(parameter)
                }
            }
            VariableExpression queryVar = varX('$query')
            // def query = new DetachedCriteria(Foo)
            body.addStatement(
                declS(queryVar, ctorX(ClassHelper.make(DetachedCriteria), args(classX(domainClassNode.plainNodeReference))))
            )

            body.addStatement(
                assignS(queryVar, callX(queryVar, "build", closureExpression))
            )
            body.addStatement(
                returnS(callX(queryVar, "find", argsExpression != null ? argsExpression : AstUtils.ZERO_ARGUMENTS))
            )
            processVariableScopes(sourceUnit, targetClassNode, newMethodNode)
        }
        else {
            AstUtils.error(sourceUnit, annotationNode, "@Where value must be a closure")
        }
    }

    @Override
    protected boolean isCompatibleReturnType(ClassNode domainClass, MethodNode methodNode, ClassNode returnType, String prefix) {
        return AstUtils.isDomainClass(returnType)
    }

    @Override
    protected Iterable<String> getHandledPrefixes() {
        return Collections.emptyList()
    }
}
