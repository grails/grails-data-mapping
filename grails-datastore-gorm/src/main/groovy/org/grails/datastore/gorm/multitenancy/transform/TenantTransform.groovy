/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.gorm.multitenancy.transform

import grails.gorm.multitenancy.CurrentTenant
import grails.gorm.multitenancy.Tenant
import grails.gorm.multitenancy.TenantService
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.grails.datastore.gorm.transform.AbstractGormASTTransformation
import org.grails.datastore.gorm.transform.AbstractMethodDecoratingTransformation
import org.springframework.core.Ordered
import org.springframework.transaction.TransactionStatus

import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import static org.codehaus.groovy.ast.ClassHelper.*
import static org.grails.datastore.mapping.reflect.AstUtils.copyParameters

/**
 * Implementation of {@link grails.gorm.multitenancy.Tenant}
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class TenantTransform extends AbstractMethodDecoratingTransformation implements Ordered {
    private static final Object APPLIED_MARKER = new Object()
    private static final ClassExpression CURRENT_TENANT_ANNOTATION_TYPE_EXPR = classX(CurrentTenant)
    private static final ClassExpression TENANT_ANNOTATION_TYPE_EXPR = classX(Tenant)
    public static final ClassNode TENANT_ANNOTATION_TYPE = TENANT_ANNOTATION_TYPE_EXPR.getType()
    public  static final ClassNode CURRENT_TENANT_ANNOTATION_TYPE = CURRENT_TENANT_ANNOTATION_TYPE_EXPR.getType()
    public static final AnnotationNode TENANT_ANNOTATION = new AnnotationNode( TENANT_ANNOTATION_TYPE )
    public static final AnnotationNode CURRENT_TENANT_ANNOTATION = new AnnotationNode( CURRENT_TENANT_ANNOTATION_TYPE )

    private static final String RENAMED_METHOD_PREFIX = '$mt__'
    public static final String VAR_TENANT_ID = "tenantId"

    @Override
    protected String getRenamedMethodPrefix() {
        return RENAMED_METHOD_PREFIX
    }

    @Override
    MethodCallExpression buildDelegatingMethodCall(SourceUnit sourceUnit, AnnotationNode annotationNode, ClassNode classNode, MethodNode methodNode, MethodCallExpression originalMethodCallExpr, BlockStatement newMethodBody) {
        ClassNode tenantServiceClassNode = make(TenantService)
        VariableExpression tenantServiceVar = varX('$tenantService', tenantServiceClassNode)
        newMethodBody.addStatement(
            declS(tenantServiceVar, callX(varX("targetDatastore"), "getService", classX(tenantServiceClassNode) ) )
        )
        return makeDelegatingClosureCall( tenantServiceVar, "withCurrent", params( param( make(Serializable), VAR_TENANT_ID)), originalMethodCallExpr)
    }

    @Override
    protected Parameter[] prepareNewMethodParameters(MethodNode methodNode) {
        final Parameter tenantIdParameter = param(make(Serializable), VAR_TENANT_ID)
        Parameter[] newParameters = methodNode.getParameters() ? (copyParameters(((methodNode.getParameters() as List) + [tenantIdParameter]) as Parameter[])) : [tenantIdParameter] as Parameter[]
        return newParameters
    }
    @Override
    protected boolean isValidAnnotation(AnnotationNode annotationNode, AnnotatedNode classNode) {
        TENANT_ANNOTATION_TYPE.equals(annotationNode.getClassNode()) || CURRENT_TENANT_ANNOTATION_TYPE.equals(annotationNode.getClassNode())
    }

    @Override
    protected ClassNode getAnnotationType() {
        return TENANT_ANNOTATION_TYPE
    }

    @Override
    protected Object getAppliedMarker() {
        return APPLIED_MARKER
    }

    @Override
    int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100
    }
}
