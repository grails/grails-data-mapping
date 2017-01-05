/* Copyright (C) 2017 original authors
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
package org.grails.datastore.gorm.transactions.transform

import grails.gorm.transactions.NotTransactional
import grails.gorm.transactions.Rollback
import grails.gorm.transactions.GrailsTransactionTemplate
import grails.gorm.transactions.Transactional
import groovy.transform.TypeChecked
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.classgen.VariableScopeVisitor
import org.codehaus.groovy.control.ErrorCollector
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.mapping.transactions.CustomizableRollbackTransactionAttribute
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.transaction.annotation.Propagation

import static org.grails.datastore.mapping.reflect.AstUtils.*
import static org.codehaus.groovy.ast.tools.GeneralUtils.*
import groovy.transform.CompileStatic

import java.lang.reflect.Modifier

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit

import org.codehaus.groovy.transform.GroovyASTTransformation
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute
import org.springframework.transaction.interceptor.RollbackRuleAttribute

/**
 * <p>This AST transform reads the {@link Transactional} annotation and transforms method calls by
 * wrapping the body of the method in an execution of {@link GrailsTransactionTemplate}.</p>
 *
 * <p>In other words given the following class:</p>
 *
 *
 * <pre>
 * class FooService {
 *   {@code @Transactional}
 *   void updateFoo() {
 *       ...
 *   }
 * }
 * </pre>
 *
 *
 * <p>The resulting byte code produced will be (more or less):</p>
 *
 * <pre>
 * class FooService {
 *   PlatformTransactionManager $transactionManager
 *
 *   PlatformTransactionManager getTransactionManager() { $transactionManager }
 *
 *   void updateFoo() {
 *       GrailsTransactionTemplate template = new GrailsTransactionTemplate(getTransactionManager())
 *       template.execute { TransactionStatus status ->
 *           $tt_updateFoo(status)
 *       }
 *   }
 *
 *   private void $tt_updateFoo(TransactionStatus status) {
 *       ...
 *   }
 * }
 * </pre>
 *
 * <p>
 *     The body of the method is moved to a new method prefixed with "$tt_" and which receives the arguments of the method and the TransactionStatus object
 * </p>
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class TransactionalTransform extends AbstractASTTransformation {
    public static final ClassNode MY_TYPE = new ClassNode(Transactional)
    public static final ClassNode COMPILE_STATIC_TYPE = ClassHelper.make(CompileStatic)
    public static final ClassNode TYPE_CHECKED_TYPE = ClassHelper.make(TypeChecked)
    private static final String PROPERTY_TRANSACTION_MANAGER = "transactionManager"
    private static final String METHOD_EXECUTE = "execute"
    private static final Set<String> METHOD_NAME_EXCLUDES = new HashSet<String>(Arrays.asList("afterPropertiesSet", "destroy"));
    private static final Set<String> ANNOTATION_NAME_EXCLUDES = new HashSet<String>(Arrays.asList(PostConstruct.class.getName(), PreDestroy.class.getName(), Transactional.class.getName(), "grails.transaction.Rollback", Rollback.class.getName(), "grails.web.controllers.ControllerMethod", NotTransactional.class.getName(), "grails.transaction.NotTransactional"));
    private static final Set<String> JUNIT_ANNOTATION_NAMES = new HashSet<String>(Arrays.asList("org.junit.Before", "org.junit.After"));
    private static final String SPEC_CLASS = "spock.lang.Specification";
    public static final String PROPERTY_DATA_SOURCE = "datasource"
    private static final Object APPLIED_MARKER = new Object();

    @Override
    void visit(ASTNode[] astNodes, SourceUnit source) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException('Internal error: wrong types: $node.class / $parent.class')
        }

        AnnotatedNode parent = (AnnotatedNode) astNodes[1];
        AnnotationNode annotationNode = (AnnotationNode) astNodes[0];
        if (!isTransactionAnnotation(annotationNode)) {
            return;
        }

        if (parent instanceof MethodNode) {
            MethodNode methodNode = (MethodNode)parent


            final declaringClassNode = methodNode.getDeclaringClass()

            weaveTransactionManagerAware(source, annotationNode, declaringClassNode)
            weaveTransactionalMethod(source, declaringClassNode, annotationNode, methodNode)
        }
        else if (parent instanceof ClassNode) {
            weaveTransactionalBehavior(source, (ClassNode)parent, annotationNode)
        }

    }

    protected boolean isTransactionAnnotation(AnnotationNode annotationNode) {
        MY_TYPE.equals(annotationNode.getClassNode()) || annotationNode.getClassNode().getName().equals("grails.transaction.Transactional")
    }

    public void weaveTransactionalBehavior(SourceUnit source, ClassNode classNode, AnnotationNode annotationNode) {
        weaveTransactionManagerAware(source, annotationNode, classNode)
        List<MethodNode> methods = new ArrayList<MethodNode>(classNode.getMethods());

        for (MethodNode md in methods) {
            String methodName = md.getName()
            int modifiers = md.modifiers
            if (!md.isSynthetic() && Modifier.isPublic(modifiers) && !Modifier.isAbstract(modifiers) &&
                    !Modifier.isStatic(modifiers) && !hasJunitAnnotation(md)) {
                if(hasExcludedAnnotation(md)) continue

                def startsWithSpock = methodName.startsWith('$spock')
                if( methodName.contains('$') && !startsWithSpock) continue

                if(startsWithSpock && methodName.endsWith('proc') ) continue

                if(md.getAnnotations().any { AnnotationNode an -> an.classNode.name == "org.spockframework.runtime.model.DataProviderMetadata"}) {
                    continue
                }

                if(METHOD_NAME_EXCLUDES.contains(methodName)) continue

                if(isSetter(md) || isGetter(md)) continue

                // don't apply to methods added by traits
                if(hasAnnotation(md, org.codehaus.groovy.transform.trait.Traits.TraitBridge.class)) continue
                // ignore methods that delegate to each other
                if(hasAnnotation(md, "grails.compiler.DelegatingMethod")) continue
                weaveTransactionalMethod(source, classNode, annotationNode, md);
            }
            else if ((("setup".equals(methodName) || "cleanup".equals(methodName)) && isSpockTest(classNode)) ||
                    hasJunitAnnotation(md)) {
                def requiresNewTransaction = new AnnotationNode(annotationNode.classNode)
                requiresNewTransaction.addMember("propagation", new PropertyExpression(new ClassExpression(ClassHelper.make(Propagation.class)), "REQUIRES_NEW"))
                weaveTransactionalMethod(source, classNode, requiresNewTransaction, md, "execute")
            }
        }
    }

    public static boolean isSpockTest(ClassNode classNode) {
        return isSubclassOf(classNode, SPEC_CLASS);
    }

    private boolean hasExcludedAnnotation(MethodNode md) {
        boolean excludedAnnotation = false;
        for (AnnotationNode annotation : md.getAnnotations()) {
            if(ANNOTATION_NAME_EXCLUDES.contains(annotation.getClassNode().getName())) {
                excludedAnnotation = true;
                break;
            }
        }
        excludedAnnotation
    }

    private boolean hasJunitAnnotation(MethodNode md) {
        boolean excludedAnnotation = false;
        for (AnnotationNode annotation : md.getAnnotations()) {
            if(JUNIT_ANNOTATION_NAMES.contains(annotation.getClassNode().getName())) {
                excludedAnnotation = true;
                break;
            }
        }
        excludedAnnotation
    }

    protected void weaveTransactionalMethod(SourceUnit source, ClassNode classNode, AnnotationNode annotationNode, MethodNode methodNode, String executeMethodName = getTransactionTemplateMethodName()) {
        if ( methodNode.getNodeMetaData(TRANSFORM_APPLIED_MARKER) == APPLIED_MARKER ) {
            return
        }

        methodNode.putNodeMetaData(TRANSFORM_APPLIED_MARKER, APPLIED_MARKER)

        MethodCallExpression originalMethodCall = moveOriginalCodeToNewMethod(source, classNode, methodNode)

        BlockStatement methodBody = block()

        final ClassNode transactionAttributeClassNode = ClassHelper.make(CustomizableRollbackTransactionAttribute)
        final VariableExpression transactionAttributeVar = varX('$transactionAttribute', transactionAttributeClassNode)
        methodBody.addStatement(
            declS(transactionAttributeVar, ctorX(transactionAttributeClassNode, ZERO_ARGUMENTS))
        )

        applyTransactionalAttributeSettings(annotationNode, transactionAttributeVar, methodBody)


        Parameter transactionStatusParam = param(ClassHelper.make(TransactionStatus), "transactionStatus")
        final Parameter[] executeMethodParameterTypes = params(transactionStatusParam)
        final callCallExpression = new ClosureExpression(executeMethodParameterTypes, createTransactionalMethodCallBody(transactionStatusParam, originalMethodCall))

        final ArgumentListExpression constructorArgs = args(
            propX(varX("this"), PROPERTY_TRANSACTION_MANAGER),
            transactionAttributeVar
        )
        final ClassNode transactionTemplateClassNode = ClassHelper.make(GrailsTransactionTemplate)
        final VariableExpression transactionTemplateVar = varX('$transactionTemplate', transactionTemplateClassNode)
        methodBody.addStatement(
            declS(
                transactionTemplateVar,
                ctorX(transactionTemplateClassNode, constructorArgs)
            )
        )

        final ArgumentListExpression methodArgs = args(callCallExpression)
        final MethodCallExpression executeMethodCallExpression = callX(
                transactionTemplateVar,
                executeMethodName,
                methodArgs)

        final Parameter[] executeMethodParameters = params( param(ClassHelper.make(Closure), null) )
        final MethodNode executeMethodNode = transactionTemplateClassNode.getMethod(executeMethodName, executeMethodParameters)
        executeMethodCallExpression.setMethodTarget(executeMethodNode)

        if(methodNode.getReturnType() != ClassHelper.VOID_TYPE) {
            methodBody.addStatement(
                returnS(
                    castX(methodNode.getReturnType(), executeMethodCallExpression)
                )
            )
        } else {
            methodBody.addStatement(
                stmt(executeMethodCallExpression)
            );
        }

        methodNode.setCode(methodBody)
        processVariableScopes(source, classNode, methodNode)
    }

    protected String getTransactionTemplateMethodName() {
        return "execute"
    }

    protected Statement createTransactionalMethodCallBody(Parameter transactionStatusParam, MethodCallExpression originalMethodCall) {
        return stmt(originalMethodCall)
    }

    protected applyTransactionalAttributeSettings(AnnotationNode annotationNode, VariableExpression transactionAttributeVar, BlockStatement methodBody) {
        final ClassNode rollbackRuleAttributeClassNode = ClassHelper.make(RollbackRuleAttribute)
        final ClassNode noRollbackRuleAttributeClassNode = ClassHelper.make(NoRollbackRuleAttribute)
        final Map<String, Expression> members = annotationNode.getMembers()
        int rollbackRuleId = 0
        members.each { String name, Expression expr ->
            if (name == 'rollbackFor' || name == 'rollbackForClassName' || name == 'noRollbackFor' || name == 'noRollbackForClassName') {
                final targetClassNode = (name == 'rollbackFor' || name == 'rollbackForClassName') ? rollbackRuleAttributeClassNode : noRollbackRuleAttributeClassNode
                name = 'rollbackRules'
                if (expr instanceof ListExpression) {
                    for(exprItem in ((ListExpression)expr).expressions) {
                        appendRuleElement(methodBody, transactionAttributeVar, name, ctorX(targetClassNode, exprItem))
                    }
                } else {
                    appendRuleElement(methodBody, transactionAttributeVar, name, ctorX(targetClassNode, expr))
                }
            } else {
                if (name == 'isolation') {
                    name = 'isolationLevel'
                    expr = callX(expr, "value", ZERO_ARGUMENTS)
                } else if (name == 'propagation') {
                    name = 'propagationBehavior'
                    expr = callX(expr, "value", ZERO_ARGUMENTS)
                }
                methodBody.addStatement(
                    assignS(propX(transactionAttributeVar, name), expr)
                )
            }
        }
    }

    private void appendRuleElement(BlockStatement methodBody, VariableExpression transactionAttributeVar, String name, Expression expr) {
        final rollbackRuleAttributeClassNode = ClassHelper.make(RollbackRuleAttribute)
        ClassNode rollbackRulesListClassNode = nonGeneric(ClassHelper.make(List), rollbackRuleAttributeClassNode)
        def getRollbackRules = castX(rollbackRulesListClassNode, buildGetPropertyExpression(transactionAttributeVar, name, transactionAttributeVar.getType()))
        methodBody.addStatement(
            stmt(
                callX(getRollbackRules, 'add', expr)
            )
        )
    }

    protected MethodCallExpression moveOriginalCodeToNewMethod(SourceUnit source, ClassNode classNode, MethodNode methodNode) {
        String renamedMethodName = '$tt__' + methodNode.getName()
        final Parameter transactionStatusParameter = param(ClassHelper.make(TransactionStatus), "transactionStatus")
        def newParameters = methodNode.getParameters() ? (copyParameters(((methodNode.getParameters() as List) + [transactionStatusParameter]) as Parameter[])) : [transactionStatusParameter] as Parameter[]


        Statement body = methodNode.code
        MethodNode renamedMethodNode = new MethodNode(
                renamedMethodName,
                Modifier.PROTECTED, methodNode.getReturnType().getPlainNodeReference(),
                newParameters,
                EMPTY_CLASS_ARRAY,
                body
        );


        def newVariableScope = new VariableScope()
        for(p in newParameters) {
            newVariableScope.putDeclaredVariable(p)
        }

        renamedMethodNode.setVariableScope(
            newVariableScope
        )

        // GrailsCompileStatic and GrailsTypeChecked are not explicitly addressed
        // here but they will be picked up because they are @AnnotationCollector annotations
        // which use CompileStatic and TypeChecked...
        renamedMethodNode.addAnnotations(methodNode.getAnnotations(COMPILE_STATIC_TYPE))
        renamedMethodNode.addAnnotations(methodNode.getAnnotations(TYPE_CHECKED_TYPE))

        methodNode.setCode(null)
        classNode.addMethod(renamedMethodNode)

        // Use a dummy source unit to process the variable scopes to avoid the issue where this is run twice producing an error
        VariableScopeVisitor scopeVisitor = new VariableScopeVisitor(new SourceUnit("dummy", "dummy", source.getConfiguration(), source.getClassLoader(), new ErrorCollector(source.getConfiguration())));
        if(methodNode == null) {
            scopeVisitor.visitClass(classNode)
        } else {
            scopeVisitor.prepareVisit(classNode)
            scopeVisitor.visitMethod(renamedMethodNode)
        }

        final originalMethodCall = callX(varX("this"), renamedMethodName, args(renamedMethodNode.parameters))
        originalMethodCall.setImplicitThis(false)
        originalMethodCall.setMethodTarget(renamedMethodNode)

        return originalMethodCall
    }


    protected void weaveTransactionManagerAware(SourceUnit source, AnnotationNode annotationNode, ClassNode declaringClassNode) {
        if ( declaringClassNode.getNodeMetaData(TRANSFORM_APPLIED_MARKER) == APPLIED_MARKER ) {
            return
        }

        declaringClassNode.putNodeMetaData(TRANSFORM_APPLIED_MARKER, APPLIED_MARKER)

        Expression connectionName = annotationNode.getMember("connection")
        boolean hasDataSourceProperty = connectionName != null

        //add the transactionManager property
        if (!hasOrInheritsProperty(declaringClassNode, PROPERTY_TRANSACTION_MANAGER) ) {

            ClassNode transactionManagerClassNode = ClassHelper.make(PlatformTransactionManager)

            String fieldName = '$' + PROPERTY_TRANSACTION_MANAGER
            FieldNode field = declaringClassNode.addField(fieldName, Modifier.PROTECTED, transactionManagerClassNode, null)

            Parameter p = param(transactionManagerClassNode, PROPERTY_TRANSACTION_MANAGER)
            Parameter[] parameters = params(p)


            VariableExpression transactionManagerPropertyExpr = varX(field)
            BlockStatement getterBody = block()

            // this is a hacky workaround that ensures the transaction manager is also set on the spock shared instance which seems to differ for
            // some reason
            if(isSubclassOf(declaringClassNode, "spock.lang.Specification")) {
                getterBody.addStatement(
                    stmt(
                        callX( propX( propX( varThis(), "specificationContext"), "sharedInstance"),
                              "setTransactionManager",
                               transactionManagerPropertyExpr)
                    )
                )
            }

            ClassExpression gormEnhancerExpr = classX(GormEnhancer)
            MethodCallExpression transactionManagerLookupExpr = callX(gormEnhancerExpr, "findSingleTransactionManager")
            transactionManagerLookupExpr.setMethodTarget(
                gormEnhancerExpr.getType().getDeclaredMethod("findSingleTransactionManager", ZERO_PARAMETERS )
            )
            Statement ifElse = ifElseS(
                notNullX( transactionManagerPropertyExpr ),
                    returnS( transactionManagerPropertyExpr ),
                    returnS(transactionManagerLookupExpr)
            )

            getterBody.addStatement( ifElse )
            declaringClassNode.addMethod("getTransactionManager",
                                            Modifier.PUBLIC,
                                            transactionManagerClassNode,
                                            ZERO_PARAMETERS, null,
                                            getterBody)

            Statement setterBody = assignS(transactionManagerPropertyExpr, varX(p))
            MethodNode methodNode = declaringClassNode.addMethod("setTransactionManager",
                                                            Modifier.PUBLIC,
                                                            ClassHelper.VOID_TYPE,
                                                            parameters,
                                                            null,
                                                            setterBody)

            AnnotationNode autowired = addAnnotationOrGetExisting(methodNode, Autowired)
            autowired.setMember("required", constX(false))
            if(hasDataSourceProperty) {
                def qualifier = addAnnotationOrGetExisting(methodNode, Qualifier)
                qualifier.addMember("value", constX("transactionManager_" + connectionName.text))
            }
        }
    }
}
