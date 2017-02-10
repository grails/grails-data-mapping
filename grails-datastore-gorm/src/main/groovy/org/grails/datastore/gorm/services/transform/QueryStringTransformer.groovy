package org.grails.datastore.gorm.services.transform

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.SourceUnit
import org.grails.datastore.mapping.reflect.AstUtils

import static org.codehaus.groovy.ast.tools.GeneralUtils.constX
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX

/**
 * This class handles type checking of HQL queries declared in {@link grails.gorm.services.Query} annotations
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
class QueryStringTransformer extends ClassCodeExpressionTransformer {

    final SourceUnit sourceUnit
    final VariableScope variableScope
    final Map<String, ClassNode> declaredQueryTargets = [:]

    QueryStringTransformer(SourceUnit sourceUnit, VariableScope variableScope) {
        this.sourceUnit = sourceUnit
        this.variableScope = variableScope
    }

    GStringExpression transformQuery(GStringExpression query) {
        Expression transformed = transform(query)
        transformed = transformPropertyExpressions(transformed)
        GStringExpression transformedGString = (GStringExpression)transformed

        int i = 0
        List<ConstantExpression> newStrings = []
        List<Expression> newValues = []
        ConstantExpression currentConstant
        List<Expression> values = transformedGString.values
        for(ConstantExpression exp in transformedGString.strings) {
            if(i < values.size()) {
                Expression valueExpr = values[i++]
                if(valueExpr instanceof ConstantExpression) {
                    ConstantExpression valueConstant = (ConstantExpression)valueExpr
                    String newConstant = exp.value.toString() + valueConstant.value.toString()
                    if(currentConstant != null) {
                        currentConstant = constX(currentConstant.value.toString() + newConstant )
                    }
                    else {
                        currentConstant = constX(newConstant)
                    }
                }
                else if(currentConstant != null) {
                    currentConstant = constX(currentConstant.value.toString() + exp.text)
                    newStrings.add(currentConstant)
                    newValues.add(valueExpr)
                    currentConstant = null
                }
            }
            else {
                newStrings.add(exp)
            }
        }

        return new GStringExpression(transformedGString.text, newStrings, newValues)
    }

    @Override
    Expression transform(Expression exp) {
        if(exp instanceof ClassExpression) {
            ClassNode type = ((ClassExpression) exp).type
            if(AstUtils.isDomainClass(type)) {
                return constX(type.name)
            }
            else {
                AstUtils.error(sourceUnit, exp, "Invalid query class [$type.name]. Referenced classes in queries must be domain classes")
            }
        }
        else if(exp instanceof PropertyExpression) {
            return transformPropertyExpressions(exp)
        }
        else if(exp instanceof MethodCallExpression) {
            MethodCallExpression mce = (MethodCallExpression)exp
            Expression methodTarget = mce.objectExpression
            if( methodTarget instanceof ClosureExpression && mce.methodAsString == 'call') {
                ClosureExpression closureExpression = (ClosureExpression)methodTarget
                Statement body = closureExpression.code
                if(body instanceof BlockStatement) {
                    def statements = ((BlockStatement) body).statements
                    if(statements.size() == 1) {
                        for(stmt in statements) {
                            if(stmt instanceof ExpressionStatement) {
                                def stmtExpr = ((ExpressionStatement)stmt).expression
                                if(stmtExpr instanceof DeclarationExpression) {
                                    DeclarationExpression dec = (DeclarationExpression)stmtExpr
                                    if(dec.leftExpression instanceof VariableExpression && dec.rightExpression instanceof EmptyExpression) {
                                        VariableExpression declaredVar = (VariableExpression)dec.leftExpression
                                        ClassNode variableType = declaredVar.type
                                        String variableName = declaredVar.name
                                        declaredQueryTargets.put(variableName, variableType)
                                        if(AstUtils.isDomainClass(variableType)) {
                                            return constX("${variableType.name} as $variableName".toString())
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
        if(exp instanceof VariableExpression) {
            VariableExpression var = (VariableExpression)exp
            def declared = variableScope.getDeclaredVariable(var.name)
            if(declared != null) {
                return varX(declared)
            }
        }
        return super.transform(exp)
    }

    Expression transformPropertyExpressions(Expression exp) {
        if(exp instanceof PropertyExpression) {
            PropertyExpression pe = (PropertyExpression)exp
            Expression targetObject = pe.objectExpression
            if( targetObject instanceof VariableExpression) {
                VariableExpression var = (VariableExpression)targetObject
                ClassNode domainType = declaredQueryTargets.get(var.name)
                if(domainType != null) {
                    String propertyName = pe.propertyAsString
                    if( AstUtils.hasProperty(domainType, propertyName) ) {
                        return constX("${var.name}.$propertyName".toString())
                    }
                    else {
                        AstUtils.error(sourceUnit, exp, "No property [$propertyName] existing for domain class [$domainType.name]")
                    }
                }
            }
        }
        return super.transform(exp)
    }
}
