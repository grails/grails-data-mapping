package org.grails.datastore.gorm.services.implementers

import grails.gorm.DetachedCriteria
import grails.gorm.services.Where
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.SourceUnit
import org.grails.datastore.gorm.query.transform.DetachedCriteriaTransformer
import org.grails.datastore.mapping.reflect.AstUtils

import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.grails.datastore.mapping.reflect.AstUtils.processVariableScopes

/**
 * Abstract implementation for queries annotated with {@link Where}
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
abstract class AbstractWhereImplementer extends AbstractReadOperationImplementer{


    public static final int POSITION = FindByImplementer.POSITION - 100

    @Override
    int getOrder() {
        return POSITION
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
            transformer.transformClosureExpression( domainClassNode, closureExpression)

            BlockStatement body = (BlockStatement)newMethodNode.getCode()

            Expression argsExpression = findArgsExpression(newMethodNode)
            VariableExpression queryVar = varX('$query')
            // def query = new DetachedCriteria(Foo)
            body.addStatement(
                    declS(queryVar, ctorX(getDetachedCriteriaType(domainClassNode), args(classX(domainClassNode.plainNodeReference))))
            )

            body.addStatement(
                    assignS(queryVar, callX(queryVar, "build", closureExpression))
            )
            Expression queryExpression = callX(queryVar, getQueryMethodToExecute(domainClassNode, newMethodNode), argsExpression != null ? argsExpression : AstUtils.ZERO_ARGUMENTS)
            body.addStatement(
                buildReturnStatement(domainClassNode, abstractMethodNode, newMethodNode, queryExpression)
            )
            processVariableScopes(sourceUnit, targetClassNode, newMethodNode)
        }
        else {
            AstUtils.error(sourceUnit, annotationNode, "@Where value must be a closure")
        }
    }

    protected ClassNode getDetachedCriteriaType(ClassNode domainClassNode) {
        ClassHelper.make(DetachedCriteria)
    }

    protected Statement buildReturnStatement(ClassNode domainClass, MethodNode abstractMethodNode, MethodNode methodNode, Expression queryExpression) {
        returnS(queryExpression)
    }

    protected String getQueryMethodToExecute(ClassNode domainClass, MethodNode newMethodNode) {
        "find"
    }

    @Override
    protected Iterable<String> getHandledPrefixes() {
        return Collections.emptyList()
    }
}
