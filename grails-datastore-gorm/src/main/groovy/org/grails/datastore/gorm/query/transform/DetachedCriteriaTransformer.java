/* Copyright (C) 2011 SpringSource
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
package org.grails.datastore.gorm.query.transform;

import grails.gorm.DetachedCriteria;
import grails.persistence.Entity;
import grails.util.GrailsNameUtils;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BitwiseNegationExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NotExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.RangeExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.CaseStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.SwitchStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.ast.stmt.WhileStatement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.LocatedMessage;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.syntax.Token;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.criteria.FunctionCallingCriterion;

/**
 * ClassCodeVisitorSupport that transforms where methods into detached criteria queries
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public class DetachedCriteriaTransformer extends ClassCodeVisitorSupport {

    private static final Class<?>[] EMPTY_JAVA_CLASS_ARRAY = {};
    private static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");
    private static final VariableExpression DELEGATE_EXPRESSION = new VariableExpression("delegate");
    public static final String AND_OPERATOR = "&";
    public static final String OR_OPERATOR = "|";
    public static final ClassNode DETACHED_CRITERIA_CLASS_NODE = new ClassNode(DetachedCriteria.class);
    public static final Set<String> CANDIDATE_METHODS_WHERE_ONLY = newSet("where");
    public static final ClassNode FUNCTION_CALL_CRITERION = new ClassNode(FunctionCallingCriterion.class);
    public static final String EQUALS_OPERATOR = "==";
    public static final String IS_NULL_CRITERION = "isNull";
    public static final ConstantExpression WHERE_LAZY = new ConstantExpression("whereLazy");

    private SourceUnit sourceUnit;
    private static final Set<String> CANDIDATE_METHODS = newSet("where", "whereLazy", "whereAny", "findAll", "find");

    private static final Set<String> SUPPORTED_FUNCTIONS = newSet(
            "lower", "upper", "trim", "length", "second",
            "hour", "minute", "day", "month", "year");

    private static final Map<String, String> OPERATOR_TO_CRITERIA_METHOD_MAP = newMap(
        "==", "eq",
        "!=", "ne",
        ">", "gt",
        "<", "lt",
        ">=", "ge",
        "<=", "le",
        "==~", "like",
        "=~", "ilike",
        "in", "inList");

    private static final Map<String, ClassNode> OPERATOR_TO_CRITERION_METHOD_MAP = newMap(
        "==", new ClassNode(Query.Equals.class),
        "!=", new ClassNode(Query.NotEquals.class),
        ">", new ClassNode(Query.GreaterThan.class),
        "<", new ClassNode(Query.LessThan.class),
        ">=", new ClassNode(Query.GreaterThanEquals.class),
        "<=", new ClassNode(Query.LessThanEquals.class),
        "==~", new ClassNode(Query.Like.class),
        "=~", new ClassNode(Query.ILike.class),
        "in", new ClassNode(Query.In.class));

    private static final Map<String, String> PROPERTY_COMPARISON_OPERATOR_TO_CRITERIA_METHOD_MAP = newMap(
        "==", "eqProperty",
        "!=", "neProperty",
        ">", "gtProperty",
        "<", "ltProperty",
        ">=", "geProperty",
        "<=", "leProperty");

    private static final Map<String, String> SIZE_OPERATOR_TO_CRITERIA_METHOD_MAP = newMap(
        "==", "sizeEq",
        "!=", "sizeNe",
        ">", "sizeGt",
        "<", "sizeLt",
        ">=", "sizeGe",
        "<=", "sizeLe");

    private static final Map<String, String> AGGREGATE_FUNCTIONS = newMap(
        "avg", "avg",
        "max", "max",
        "min", "min",
        "sum", "sum",
        "property", "property",
        "count", "countDistinct");

    private Map<String, ClassNode> detachedCriteriaVariables = new HashMap<String, ClassNode>();
    private Map<String, ClassNode> staticDetachedCriteriaVariables = new HashMap<String, ClassNode>();
    private Map<String, Map<String,ClassNode>> cachedClassProperties = new HashMap<String, Map<String,ClassNode>>();
    private Set<ClosureExpression> transformedExpressions = new HashSet<ClosureExpression>();
    private ClassNode currentClassNode;

    DetachedCriteriaTransformer(SourceUnit sourceUnit) {
        this.sourceUnit = sourceUnit;
    }

    @Override
    public void visitClass(ClassNode node) {
        try {
            this.currentClassNode = node;
            super.visitClass(node);
        } catch(Exception e) {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            sourceUnit.getErrorCollector().addError(new LocatedMessage("Fatal error occurred apply query transformations: " + e.getMessage(), Token.newString(node.getName(),node.getLineNumber(), node.getColumnNumber()), sourceUnit));
        } finally {
            currentClassNode = null;
            detachedCriteriaVariables.clear();
            transformedExpressions.clear();
        }
    }

    @Override
    public void visitMethod(MethodNode node) {
        try {
            super.visitMethod(node);
        } finally {
            detachedCriteriaVariables.clear();
        }
    }

    @Override
    public void visitField(FieldNode node) {
        ClassNode classNode = node.getOwner();
        if (node.isStatic() && isDomainClass(classNode)) {
            Expression initialExpression = node.getInitialExpression();
            if (initialExpression instanceof MethodCallExpression) {
                MethodCallExpression mce = (MethodCallExpression) initialExpression;

                if (isCandidateWhereMethod(mce.getMethod(), mce.getArguments())) {
                    ArgumentListExpression args = (ArgumentListExpression) mce.getArguments();
                    List<Expression> argsExpressions = args.getExpressions();
                    int totalExpressions = argsExpressions.size();
                    if (totalExpressions > 0) {
                        Expression expression = argsExpressions.get(totalExpressions - 1);
                        if (expression instanceof ClosureExpression) {
                            ClosureExpression closureExpression = (ClosureExpression) expression;
                            transformClosureExpression(classNode, closureExpression);

                            String buildMethod = mce.getMethodAsString().equals("whereLazy") ? "buildLazy" : "build";
                            MethodCallExpression newInitialExpression = new MethodCallExpression(new ConstructorCallExpression(DETACHED_CRITERIA_CLASS_NODE, new ArgumentListExpression(new ClassExpression(classNode))), buildMethod, new ArgumentListExpression(closureExpression));
                            node.setInitialValueExpression(newInitialExpression);
                            node.setType(DETACHED_CRITERIA_CLASS_NODE);
                            staticDetachedCriteriaVariables.put(node.getName(), classNode);
                        }
                    }
                }
            }
        }
        else {
            try {
                Expression initialExpression = node.getInitialExpression();
                ClosureExpression newClosureExpression = handleDetachedCriteriaCast(initialExpression);

                if (newClosureExpression != null) {
                    node.setInitialValueExpression(newClosureExpression);
                }
            } catch (Exception e) {
                sourceUnit.getErrorCollector().addError(new LocatedMessage("Fatal error occurred applying query transformations: " + e.getMessage(), Token.newString(node.getName(), node.getLineNumber(), node.getColumnNumber()), sourceUnit));
            }
        }

        super.visitField(node);
    }

    @Override
    public void visitDeclarationExpression(DeclarationExpression expression) {
        Expression initializationExpression = expression.getRightExpression();
        if (initializationExpression instanceof MethodCallExpression) {
            MethodCallExpression call = (MethodCallExpression) initializationExpression;
            Expression objectExpression = call.getObjectExpression();
            Expression method = call.getMethod();
            Expression arguments = call.getArguments();
            if (isCandidateMethod(method.getText(), arguments, CANDIDATE_METHODS_WHERE_ONLY)) {
                ClassNode classNode = new ClassNode(DetachedCriteria.class);
                ClassNode targetType = objectExpression.getType();
                if (isDomainClass(targetType)) {
                    classNode.setGenericsTypes(new GenericsType[]{new GenericsType(targetType)});

                    VariableExpression variableExpression = expression.getVariableExpression();
                    if (variableExpression.isClosureSharedVariable()) {
                        Variable accessedVariable = variableExpression.getAccessedVariable();
                        if (accessedVariable instanceof VariableExpression) {
                            ((VariableExpression)accessedVariable).setType(classNode);
                        }
                    }
                    else {
                        variableExpression.setType(classNode);
                    }
                    String variableName = expression.getVariableExpression().getName();
                    detachedCriteriaVariables.put(variableName, targetType);
                }
            }
        }
        else if (initializationExpression instanceof ConstructorCallExpression) {
            String variableName = expression.getVariableExpression().getName();
            ConstructorCallExpression cce = (ConstructorCallExpression) initializationExpression;

            ClassNode type = cce.getType();
            if (DETACHED_CRITERIA_CLASS_NODE.getName().equals(type.getName())) {
                Expression arguments = cce.getArguments();
                if (arguments instanceof ArgumentListExpression) {
                    ArgumentListExpression ale = (ArgumentListExpression) arguments;
                    if (ale.getExpressions().size() == 1) {
                        Expression exp = ale.getExpression(0);
                        if (exp instanceof ClassExpression) {
                            ClassExpression clse = (ClassExpression) exp;
                            detachedCriteriaVariables.put(variableName, clse.getType());
                        }
                    }
                }
            }
        }
        else {
            try {
                ClosureExpression newClosureExpression = handleDetachedCriteriaCast(initializationExpression);
                if (newClosureExpression != null) {
                    expression.setRightExpression(newClosureExpression);
                }
            } catch (Exception e) {
                sourceUnit.getErrorCollector().addError(new LocatedMessage("Fatal error occurred applying query transformations [ " + e.getMessage() + "] to source ["+sourceUnit.getName()+"]. Please report an issue.", Token.newString(initializationExpression.getText(), initializationExpression.getLineNumber(), initializationExpression.getColumnNumber()), sourceUnit));
            }
        }
        super.visitDeclarationExpression(expression);
    }

    private ClosureExpression handleDetachedCriteriaCast(Expression initializationExpression) {
        ClosureExpression newClosureExpression = null;
        if ((initializationExpression instanceof CastExpression) && ((CastExpression) initializationExpression).getExpression() instanceof ClosureExpression) {
            CastExpression ce = (CastExpression) initializationExpression;
            Expression castTarget = ce.getExpression();
            ClosureExpression cle = (ClosureExpression) castTarget;
            ClassNode targetCastType = ce.getType();
            if (targetCastType.getName().equals(DetachedCriteria.class.getName())) {
                GenericsType[] genericsTypes = targetCastType.getGenericsTypes();
                if (genericsTypes.length > 0) {
                    ClassNode genericType = genericsTypes[0].getType();
                    transformClosureExpression(genericType, cle);
                    newClosureExpression = cle;
                }
            }
        }
        return newClosureExpression;
    }

    @Override
    public void visitMethodCallExpression(MethodCallExpression call) {
            Expression objectExpression = call.getObjectExpression();
        Expression method = call.getMethod();
        Expression arguments = call.getArguments();
        try {
            if (isCandidateMethodCallForTransform(objectExpression, method, arguments)) {
                ClassExpression ce = getTargetClassExpresssion(objectExpression);
                if (ce != null) {
                    ClassNode classNode = ce.getType();
                    this.currentClassNode = classNode;
                    visitMethodCall(classNode, arguments);
                }
            }
            else if (objectExpression instanceof VariableExpression) {
                VariableExpression var = (VariableExpression) objectExpression;
                String varName = var.getName();

                ClassNode varType = detachedCriteriaVariables.get(varName);
                if (varType != null && isCandidateWhereMethod(method, arguments)) {
                    this.currentClassNode = varType;
                    visitMethodCall(varType, arguments);
                }
                else if (THIS_EXPRESSION.getName().equals(varName) && currentClassNode != null && isCandidateWhereMethod(method.getText(), arguments)) {
                    visitMethodCall(this.currentClassNode, arguments);
                    call.setMethod(WHERE_LAZY);
                }
            }
            else if (objectExpression instanceof PropertyExpression) {
                PropertyExpression pe = (PropertyExpression) objectExpression;
                String propName = pe.getPropertyAsString();
                ClassNode classNode = pe.getObjectExpression().getType();
                if (isDomainClass(classNode)) {
                    ClassNode propertyType = getPropertyType(classNode, propName);
                    if (propertyType != null && DETACHED_CRITERIA_CLASS_NODE.equals(propertyType)) {
                        visitMethodCall(classNode, arguments);
                    }
                }
            }
        } catch (Exception e) {
            sourceUnit.getErrorCollector().addError(new LocatedMessage("Fatal error occurred applying query transformations: " + e.getMessage(),
                    Token.newString(call.getMethodAsString(), call.getLineNumber(), call.getColumnNumber()), sourceUnit));
        }
        super.visitMethodCallExpression(call);
    }

    private ClassExpression getTargetClassExpresssion(Expression objectExpression) {
        if (objectExpression instanceof ClassExpression) {
            return (ClassExpression) objectExpression;
        }
        if (objectExpression instanceof MethodCallExpression) {
            MethodCallExpression mce = (MethodCallExpression) objectExpression;

            Expression oe = mce.getObjectExpression();
            if (oe instanceof ClassExpression) {
                return (ClassExpression) oe;
            }
        }
        return null;
    }

    private boolean isCandidateMethodCallForTransform(Expression objectExpression, Expression method, Expression arguments) {
        return ((objectExpression instanceof ClassExpression) || isObjectExpressionWhereCall(objectExpression)) &&
                isCandidateWhereMethod(method, arguments);
    }

    private boolean isObjectExpressionWhereCall(Expression objectExpression) {
        if (objectExpression instanceof MethodCallExpression) {
            MethodCallExpression mce = (MethodCallExpression) objectExpression;
            return isCandidateWhereMethod(mce.getMethodAsString(),mce.getArguments());
        }
        return false;
    }

    private void visitMethodCall(ClassNode classNode, Expression arguments) {
        if (isDomainClass(classNode) && (arguments instanceof ArgumentListExpression)) {
            visitMethodCallOnDetachedCriteria(classNode, (ArgumentListExpression)arguments);
        }
    }

    private void visitMethodCallOnDetachedCriteria(ClassNode classNode, ArgumentListExpression arguments) {
        if (arguments.getExpressions().size() > 0) {
            Expression expression = arguments.getExpression(arguments.getExpressions().size()-1);
            if (expression instanceof ClosureExpression) {
                ClosureExpression closureExpression = (ClosureExpression) expression;
                transformClosureExpression(classNode, closureExpression);
            }
        }
    }

    private boolean isCandidateWhereMethod(Expression method, Expression arguments) {
        String methodName = method.getText();
        return ((method instanceof ConstantExpression) && isCandidateWhereMethod(methodName, arguments));
    }

    private boolean isCandidateWhereMethod(String methodName, Expression arguments) {
        return isCandidateMethod(methodName, arguments, CANDIDATE_METHODS);
    }

    private boolean isCandidateMethod(String methodName, Expression arguments, Set<String> candidateMethods) {
        return (candidateMethods.contains(methodName)) && (arguments instanceof ArgumentListExpression);
    }

    @Override
    public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
        String method = call.getMethod();
        Expression arguments = call.getArguments();
        if (isCandidateWhereMethod(method,arguments)) {
            ClassNode classNode = call.getOwnerType();
            visitMethodCall(classNode, arguments);
        }
        super.visitStaticMethodCallExpression(call);
    }

    protected void transformClosureExpression(ClassNode classNode, ClosureExpression closureExpression) {
        if (transformedExpressions.contains(closureExpression)) return;
        List<String> propertyNames = getPropertyNames(classNode);
        Statement code = closureExpression.getCode();
        BlockStatement newCode = new BlockStatement();
        boolean addAll = false;

        if (code instanceof BlockStatement) {
            BlockStatement bs = (BlockStatement) code;

            addBlockStatementToNewQuery(bs, newCode, addAll, propertyNames, closureExpression.getVariableScope());
            newCode.setVariableScope(bs.getVariableScope());
        }

        if (!newCode.getStatements().isEmpty()) {
            transformedExpressions.add(closureExpression);
            closureExpression.setCode(newCode);
        }
    }

    private List<String> getPropertyNames(ClassNode classNode) {
        String className = classNode.getName();
        Map<String, ClassNode> cachedProperties = cachedClassProperties.get(className);
        if (cachedProperties == null) {
            cachedProperties = new HashMap<String, ClassNode>();
            cachedProperties.put(GrailsDomainClassProperty.IDENTITY, new ClassNode(Long.class));
            cachedProperties.put(GrailsDomainClassProperty.VERSION, new ClassNode(Long.class));
            cachedClassProperties.put(className, cachedProperties);
            ClassNode currentNode = classNode;
            while(currentNode != null && !currentNode.equals(ClassHelper.OBJECT_TYPE)) {
                populatePropertiesForClassNode(currentNode, cachedProperties);
                currentNode = currentNode.getSuperClass();
            }
        }
        return new ArrayList<String>(cachedProperties.keySet());
    }

    private void populatePropertiesForClassNode(ClassNode classNode, Map<String, ClassNode> cachedProperties) {
        List<MethodNode> methods = classNode.getMethods();
        for (MethodNode method : methods) {
            if (!method.isAbstract() && !method.isStatic() && isGetter(method.getName(), method)) {
                String propertyName = GrailsClassUtils.getPropertyForGetter(method.getName());
                cachedProperties.put(propertyName, method.getReturnType());
            }
        }
        List<PropertyNode> properties = classNode.getProperties();
        for (PropertyNode property : properties) {

            String propertyName = property.getName();
            if (GrailsDomainClassProperty.HAS_MANY.equals(propertyName) || GrailsDomainClassProperty.BELONGS_TO.equals(propertyName) || GrailsDomainClassProperty.HAS_ONE.equals(propertyName)) {
                Expression initialExpression = property.getInitialExpression();
                if (initialExpression instanceof MapExpression) {
                    MapExpression me = (MapExpression) initialExpression;
                    List<MapEntryExpression> mapEntryExpressions = me.getMapEntryExpressions();
                    for (MapEntryExpression mapEntryExpression : mapEntryExpressions) {
                        Expression keyExpression = mapEntryExpression.getKeyExpression();
                        Expression valueExpression = mapEntryExpression.getValueExpression();
                        if (valueExpression instanceof ClassExpression) {
                            cachedProperties.put(keyExpression.getText(), valueExpression.getType());
                        }
                    }
                }
            }
            else {
                cachedProperties.put(propertyName, property.getType());
            }
        }
    }

    private void addBlockStatementToNewQuery(BlockStatement blockStatement, BlockStatement newCode, boolean addAll, List<String> propertyNames, VariableScope variableScope) {
        List<Statement> statements = blockStatement.getStatements();
        for (Statement statement : statements) {
            addStatementToNewQuery(statement, newCode, addAll, propertyNames, variableScope);
        }
    }

    private void addStatementToNewQuery(Statement statement, BlockStatement newCode, boolean addAll, List<String> propertyNames, VariableScope variableScope) {
        if (statement instanceof BlockStatement) {
             addBlockStatementToNewQuery((BlockStatement)statement, newCode, addAll, propertyNames, variableScope);
        }
        else if (statement instanceof ExpressionStatement) {
            ExpressionStatement es = (ExpressionStatement) statement;

            Expression expression = es.getExpression();
            if (expression instanceof DeclarationExpression) {
                newCode.addStatement(es);
            }
            else if (expression instanceof BinaryExpression) {
                BinaryExpression be = (BinaryExpression) expression;
                addBinaryExpressionToNewBody(propertyNames, newCode, be, addAll, variableScope);
            } else if (expression instanceof NotExpression) {
                NotExpression not = (NotExpression) expression;

                handleNegation(propertyNames, newCode, not, variableScope);
            } else if (expression instanceof MethodCallExpression) {
                MethodCallExpression methodCall = (MethodCallExpression) expression;

                handleAssociationMethodCallExpression(newCode, methodCall, propertyNames, variableScope);
            }
        }
        else {
            if (statement instanceof IfStatement) {
                IfStatement ifs = (IfStatement) statement;
                Statement ifb = ifs.getIfBlock();
                BlockStatement newIfBlock = new BlockStatement();
                addStatementToNewQuery(ifb, newIfBlock, addAll, propertyNames, variableScope);
                ifs.setIfBlock(flattenStatementIfNecessary(newIfBlock));

                Statement elseBlock = ifs.getElseBlock();
                if (elseBlock != null) {
                    BlockStatement newElseBlock = new BlockStatement();
                    addStatementToNewQuery(elseBlock, newElseBlock, addAll, propertyNames, variableScope);
                    ifs.setElseBlock(flattenStatementIfNecessary(newElseBlock));
                }
                newCode.addStatement(ifs);
            }
            else if (statement instanceof SwitchStatement) {
                SwitchStatement sw = (SwitchStatement) statement;

                List<CaseStatement> caseStatements = sw.getCaseStatements();
                for (CaseStatement caseStatement : caseStatements) {
                    Statement existingCode = caseStatement.getCode();
                    BlockStatement newCaseCode = new BlockStatement();
                    addStatementToNewQuery(existingCode, newCaseCode, addAll, propertyNames, variableScope);
                    caseStatement.setCode(flattenStatementIfNecessary(newCaseCode));
                }

                newCode.addStatement(sw);
            }
            else if (statement instanceof ForStatement) {
                ForStatement fs = (ForStatement) statement;
                Statement loopBlock = fs.getLoopBlock();
                BlockStatement newLoopBlock = new BlockStatement();
                addStatementToNewQuery(loopBlock, newLoopBlock, addAll, propertyNames, variableScope);
                fs.setLoopBlock(flattenStatementIfNecessary(newLoopBlock));
                newCode.addStatement(fs);
            }
            else if (statement instanceof WhileStatement) {
                WhileStatement ws = (WhileStatement) statement;
                Statement loopBlock = ws.getLoopBlock();
                BlockStatement newLoopBlock = new BlockStatement();
                addStatementToNewQuery(loopBlock, newLoopBlock, addAll, propertyNames, variableScope);
                ws.setLoopBlock(flattenStatementIfNecessary(newLoopBlock));
                newCode.addStatement(ws);
            }
            else if (statement instanceof TryCatchStatement) {
                TryCatchStatement tcs = (TryCatchStatement) statement;
                Statement tryStatement = tcs.getTryStatement();

                BlockStatement newTryStatement = new BlockStatement();
                addStatementToNewQuery(tryStatement, newTryStatement, addAll, propertyNames, variableScope);
                tcs.setTryStatement(flattenStatementIfNecessary(newTryStatement));

                List<CatchStatement> catchStatements = tcs.getCatchStatements();

                for (CatchStatement catchStatement : catchStatements) {
                    BlockStatement newCatchStatement = new BlockStatement();
                    Statement code = catchStatement.getCode();
                    addStatementToNewQuery(code, newCatchStatement, addAll, propertyNames, variableScope);
                    catchStatement.setCode(flattenStatementIfNecessary(newCatchStatement));
                }

                Statement finallyStatement = tcs.getFinallyStatement();
                if (finallyStatement != null) {
                    BlockStatement newFinallyStatement = new BlockStatement();
                    addStatementToNewQuery(finallyStatement, newFinallyStatement, addAll, propertyNames, variableScope);
                    tcs.setFinallyStatement(flattenStatementIfNecessary(newFinallyStatement));
                }
                newCode.addStatement(tcs);
            }
            else {
                newCode.addStatement(statement);
            }
        }
    }

    private Statement flattenStatementIfNecessary(BlockStatement blockStatement) {
        if (blockStatement.getStatements().size() == 1) {
            return blockStatement.getStatements().get(0);
        }
        return blockStatement;
    }

    private void handleAssociationMethodCallExpression(BlockStatement newCode, MethodCallExpression methodCall, List<String> propertyNames, VariableScope variableScope) {
        Expression method = methodCall.getMethod();
        String methodName = method.getText();
        ArgumentListExpression arguments = methodCall.getArguments() instanceof ArgumentListExpression ? (ArgumentListExpression) methodCall.getArguments() : null;

        if (methodName.equals("call") && hasClosureArgument(arguments)) {
            methodName = methodCall.getObjectExpression().getText();
        }
        if (isAssociationMethodCall(propertyNames, methodName, arguments)) {
            ClosureAndArguments closureAndArguments = new ClosureAndArguments(variableScope);
            ClosureExpression associationQuery = (ClosureExpression) arguments.getExpression(0);
            BlockStatement currentBody = closureAndArguments.getCurrentBody();
            ArgumentListExpression argList = closureAndArguments.getArguments();
            newCode.addStatement(new ExpressionStatement(new MethodCallExpression(DELEGATE_EXPRESSION, methodName, argList)));
            Statement associationCode = associationQuery.getCode();
            if (associationCode instanceof BlockStatement) {

                List<String> associationPropertyNames = null;
                ClassNode type = getPropertyType(methodName);
                if (!isDomainClass(type)) {
                    ClassNode associationTypeFromGenerics = getAssociationTypeFromGenerics(type);
                    if (associationTypeFromGenerics != null) {
                        type = associationTypeFromGenerics;
                        associationPropertyNames = getPropertyNamesForAssociation(associationTypeFromGenerics);
                    }
                }
                if (associationPropertyNames == null) {
                    associationPropertyNames = getPropertyNames(type);
                }

                ClassNode existing = currentClassNode;
                try {
                    if (!associationPropertyNames.isEmpty() && !isDomainClass(type)) {

                        type = getAssociationTypeFromGenerics(type);
                        if (type != null) {
                            associationPropertyNames = getPropertyNames(type);
                        }
                    }

                    currentClassNode = type;
                    addBlockStatementToNewQuery((BlockStatement) associationCode, currentBody, associationPropertyNames.isEmpty(), associationPropertyNames,variableScope);
                } finally {
                    currentClassNode = existing;
                }
            }
        }
//        else {
//            sourceUnit.getErrorCollector().addError(new LocatedMessage("Method call ["+methodName+"] is invalid. Only binary expressions are allowed in queries.", Token.newString(methodName,methodCall.getLineNumber(), methodCall.getColumnNumber()), sourceUnit));
//        }
    }

    private List<String> getPropertyNamesForAssociation(ClassNode type) {
        List<String> associationPropertyNames = Collections.emptyList();
        if (type != null) {
            if (isDomainClass(type)) {
                associationPropertyNames = getPropertyNames(type);
            }
            else {
                ClassNode associationType = getAssociationTypeFromGenerics(type);
                if (associationType != null) {
                    associationPropertyNames = getPropertyNames(associationType);
                }
            }
        }
        return associationPropertyNames;
    }

    private ClassNode getAssociationTypeFromGenerics(ClassNode type) {
        GenericsType[] genericsTypes = type.getGenericsTypes();
        ClassNode associationType = null;
        if (genericsTypes != null && genericsTypes.length == 1) {
            GenericsType genericType = genericsTypes[0];
            associationType = genericType.getType();
        }
        return associationType;
    }

    private ClassNode getPropertyType(String prop) {
        ClassNode classNode = this.currentClassNode;
        return getPropertyType(classNode, prop);
    }

    private ClassNode getPropertyType(ClassNode classNode, String prop) {
        Map<String, ClassNode> cachedProperties = cachedClassProperties.get(classNode.getName());
        if (cachedProperties != null && cachedProperties.containsKey(prop)) {
            return cachedProperties.get(prop);
        }
        ClassNode type = null;
        PropertyNode property = classNode.getProperty(prop);
        if (property != null) {
            type = property.getType();
        } else {
            MethodNode methodNode = currentClassNode.getMethod(GrailsNameUtils.getGetterName(prop), new Parameter[0]);
            if (methodNode != null) {
                type = methodNode.getReturnType();
            }
            else {
                FieldNode fieldNode = classNode.getDeclaredField(prop);
                if (fieldNode != null) {
                    type = fieldNode.getType();
                }
            }
        }
        return type;
    }

    private boolean isAssociationMethodCall(List<String> propertyNames, String methodName, ArgumentListExpression arguments) {
        return propertyNames.contains(methodName) && hasClosureArgument(arguments);
    }

    private boolean hasClosureArgument(ArgumentListExpression arguments) {
        return arguments != null && arguments.getExpressions().size() == 1 && (arguments.getExpression(0) instanceof ClosureExpression);
    }

    private void handleNegation(List<String> propertyNames, BlockStatement newCode, NotExpression not, VariableScope variableScope) {
        Expression subExpression = not.getExpression();
        if (subExpression instanceof BinaryExpression) {
            ArgumentListExpression arguments = new ArgumentListExpression();
            BlockStatement currentBody = new BlockStatement();
            ClosureExpression newClosureExpression = new ClosureExpression(new Parameter[0], currentBody);
            newClosureExpression.setVariableScope(new VariableScope());
            arguments.addExpression(newClosureExpression);
            addBinaryExpressionToNewBody(propertyNames, currentBody, (BinaryExpression) subExpression, false, variableScope);

            newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "not", arguments)));
        }
        else {
            sourceUnit.getErrorCollector().addError(new LocatedMessage("You can only negate a binary expressions in queries.", Token.newString(not.getText(),not.getLineNumber(), not.getColumnNumber()), sourceUnit));
        }
    }

    private void addBinaryExpressionToNewBody(List<String> propertyNames, BlockStatement newCode, BinaryExpression be, boolean addAll, VariableScope variableScope) {
        Token operation = be.getOperation();

        String operator = operation.getRootText();

        Expression leftExpression = be.getLeftExpression();
        Expression rightExpression = be.getRightExpression();
        if (leftExpression instanceof VariableExpression) {
            VariableExpression leftVariable = (VariableExpression) leftExpression;
            String propertyName = leftVariable.getText();
            if (propertyNames.contains(propertyName) || addAll) {
                if (OPERATOR_TO_CRITERIA_METHOD_MAP.containsKey(operator)) {
                    addCriteriaCallMethodExpression(newCode, operator, rightExpression, propertyName, propertyNames, addAll, variableScope);
                }
                else {
                    sourceUnit.getErrorCollector().addError(new LocatedMessage("Unsupported operator ["+operator+"] used in query", operation, sourceUnit));
                }
            }
            else {
                sourceUnit.getErrorCollector().addError(new LocatedMessage("Cannot query on property \""+propertyName+"\" - no such property on class "+currentClassNode.getName()+" exists.", Token.newString(propertyName,leftExpression.getLineNumber(), leftExpression.getColumnNumber()), sourceUnit));
            }
        }   else  {

            if (leftExpression instanceof MethodCallExpression) {
                MethodCallExpression mce = (MethodCallExpression) leftExpression;
                String methodName = mce.getMethodAsString();
                Expression objectExpression = mce.getObjectExpression();
                if ("size".equals(methodName) && (objectExpression instanceof VariableExpression)) {
                    String propertyName = objectExpression.getText();
                    if (propertyNames.contains(propertyName)) {
                        String sizeOperator = SIZE_OPERATOR_TO_CRITERIA_METHOD_MAP.get(operator);
                        if (sizeOperator != null) {
                              addCriteriaCall(newCode,operator,rightExpression, propertyName, propertyNames, addAll, sizeOperator, variableScope);
                        }
                        else {
                            sourceUnit.getErrorCollector().addError(new LocatedMessage("Unsupported operator ["+operator+"] used in size() query", operation, sourceUnit));
                        }
                    }
                    else {
                        sourceUnit.getErrorCollector().addError(new LocatedMessage("Cannot query size of property \""+propertyName+"\" - no such property on class "+currentClassNode.getName()+" exists.", Token.newString(propertyName,leftExpression.getLineNumber(), leftExpression.getColumnNumber()), sourceUnit));
                    }

                    return;
                }
                else {
                    boolean isFunctionCall = isFunctionCall(mce, methodName, objectExpression);
                    if (isFunctionCall) {
                        String functionName = methodName;
                        ArgumentListExpression existingArgs = (ArgumentListExpression) mce.getArguments();
                        Expression propertyNameExpression = existingArgs.getExpression(0);
                        if (propertyNameExpression instanceof PropertyExpression) {
                           handleAssociationQueryViaPropertyExpression((PropertyExpression) propertyNameExpression, rightExpression,operator,newCode,propertyNames,functionName, variableScope);
                        }
                        else {
                           handleFunctionCall(newCode, operator, rightExpression, functionName, propertyNameExpression);
                        }
                        return;
                    }
                }
            }

            String methodNameToCall = null;
            if (operator.contains(AND_OPERATOR)) {
                methodNameToCall = "and";
            } else if (operator.contains(OR_OPERATOR)) {
                methodNameToCall = "or";
            }
            ArgumentListExpression arguments = new ArgumentListExpression();
            BlockStatement currentBody = new BlockStatement();
            handleBinaryExpressionSide(leftExpression,rightExpression, operator, currentBody, addAll, propertyNames, variableScope);
            handleBinaryExpressionSide(rightExpression, rightExpression, operator,currentBody, addAll, propertyNames, variableScope);

            ClosureExpression newClosureExpression = new ClosureExpression(new Parameter[0], currentBody);
            newClosureExpression.setVariableScope(variableScope);
            arguments.addExpression(newClosureExpression);
            if (methodNameToCall != null) {
                newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, methodNameToCall, arguments)));
            }
            else {
                List<Statement> statements = currentBody.getStatements();
                for (Statement statement : statements) {
                    newCode.addStatement(statement);
                }
            }
        }
    }

    private boolean isFunctionCall(MethodCallExpression mce) {
        boolean isThis = "this".equals(mce.getObjectExpression().getText());
        Expression arguments = mce.getArguments();
        boolean hasOneArg = arguments instanceof ArgumentListExpression ? ((ArgumentListExpression)arguments).getExpressions().size() == 1 : false;
        return isThis && hasOneArg && SUPPORTED_FUNCTIONS.contains(mce.getMethodAsString());
    }

    private boolean isFunctionCall(MethodCallExpression mce, String methodName, Expression objectExpression) {
        boolean isThis = "this".equals(objectExpression.getText());
        Expression arguments = mce.getArguments();
        boolean hasOneArg = arguments instanceof ArgumentListExpression ? ((ArgumentListExpression)arguments).getExpressions().size() == 1 : false;
        return isThis && hasOneArg && SUPPORTED_FUNCTIONS.contains(methodName);
    }

    private void handleFunctionCall(BlockStatement newCode,String operator, Expression rightExpression, String functionName, Expression propertyNameExpression) {
        ArgumentListExpression newArgs = new ArgumentListExpression();
        ArgumentListExpression constructorArgs = new ArgumentListExpression();
        constructorArgs.addExpression(new ConstantExpression(functionName));
        ClassNode criterionClassNode = OPERATOR_TO_CRITERION_METHOD_MAP.get(operator);
        if (criterionClassNode != null) {
            ArgumentListExpression criterionConstructorArguments = new ArgumentListExpression();
            if (!(propertyNameExpression instanceof ConstantExpression)) {
                propertyNameExpression = new ConstantExpression(propertyNameExpression.getText());
            }
            criterionConstructorArguments.addExpression(propertyNameExpression);
            criterionConstructorArguments.addExpression(rightExpression);
            constructorArgs.addExpression(new ConstructorCallExpression(criterionClassNode, criterionConstructorArguments));
            ConstructorCallExpression constructorCallExpression = new ConstructorCallExpression(FUNCTION_CALL_CRITERION, constructorArgs);
            newArgs.addExpression(constructorCallExpression );
            newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "add", newArgs)));
        }
        else {
            sourceUnit.getErrorCollector().addError(new LocatedMessage("Unsupported operator ["+operator+"] used with function call ["+functionName+"] in query", Token.newString(functionName,rightExpression.getLineNumber(), rightExpression.getColumnNumber()), sourceUnit));
        }
    }

    private void handleBinaryExpressionSide(Expression expressionSide, Expression oppositeSide, String operator, BlockStatement newCode, boolean addAll, List<String> propertyNames, VariableScope variableScope) {
        if (expressionSide instanceof BinaryExpression) {
            addBinaryExpressionToNewBody(propertyNames, newCode, (BinaryExpression) expressionSide, addAll, variableScope);
        } else if (expressionSide instanceof NotExpression) {
            handleNegation(propertyNames, newCode, (NotExpression) expressionSide, variableScope);
        }
        else if (expressionSide instanceof MethodCallExpression) {
            MethodCallExpression methodCallExpression = (MethodCallExpression) expressionSide;
            handleAssociationMethodCallExpression(newCode, methodCallExpression, propertyNames, variableScope);
        }
        else if (expressionSide instanceof PropertyExpression) {
            PropertyExpression pe = (PropertyExpression) expressionSide;
            handleAssociationQueryViaPropertyExpression(pe, oppositeSide, operator, newCode, propertyNames, null, variableScope);
        }
        else {
            // TODO: compilation error?
        }
    }

    private void handleAssociationQueryViaPropertyExpression(PropertyExpression pe, Expression oppositeSide, String operator, BlockStatement newCode, List<String> propertyNames, String functionName, VariableScope variableScope) {
        Expression objectExpression = pe.getObjectExpression();
        if (objectExpression instanceof PropertyExpression) {
            // nested property expression, we have to find the root variable expression and walk backwards through all the properties involved
            List<String> associationMethodCalls = new ArrayList<String>();

            while(objectExpression instanceof PropertyExpression) {
                PropertyExpression currentPe = (PropertyExpression) objectExpression;
                associationMethodCalls.add(currentPe.getPropertyAsString());
                objectExpression = currentPe.getObjectExpression();
            }

            if (objectExpression instanceof VariableExpression) {
                VariableExpression ve = (VariableExpression) objectExpression;

                associationMethodCalls.add(ve.getName());

                Collections.reverse(associationMethodCalls);

                ClassNode currentType = currentClassNode;
                BlockStatement currentBody = newCode;

                for (Iterator<String> iterator = associationMethodCalls.iterator(); iterator.hasNext(); ) {
                    String associationMethodCall = iterator.next();

                    ClosureAndArguments closureAndArguments = new ClosureAndArguments(variableScope);

                    ArgumentListExpression arguments = closureAndArguments.getArguments();
                    ClassNode type = getPropertyTypeFromGenerics(associationMethodCall, currentType);

                    if (type == null) break;

                    currentType = type;
                    currentBody.addStatement(new ExpressionStatement(new MethodCallExpression(DELEGATE_EXPRESSION, associationMethodCall, arguments)));
                    currentBody = closureAndArguments.getCurrentBody();

                    if (!iterator.hasNext()) {
                        String associationProperty = pe.getPropertyAsString();
                        List<String> associationPropertyNames = getPropertyNamesForAssociation(type);
                        ClassNode existing = this.currentClassNode;
                        try {

                            this.currentClassNode = type;
                            boolean hasNoProperties = associationPropertyNames.isEmpty();
                            if (functionName != null) {
                                handleFunctionCall(currentBody, operator, oppositeSide, functionName, new ConstantExpression(associationProperty));
                            }
                            else {
                                addCriteriaCallMethodExpression(currentBody, operator, oppositeSide, associationProperty, associationPropertyNames, hasNoProperties, variableScope);
                            }
                        } finally {
                            this.currentClassNode = existing;
                        }
                    }
                }
            }
        }
        else if (objectExpression instanceof VariableExpression) {
            String propertyName = objectExpression.getText();
            if (propertyNames.contains(propertyName)) {
                String associationProperty = pe.getPropertyAsString();

                ClassNode classNode = currentClassNode;
                ClassNode type = getPropertyTypeFromGenerics(propertyName, classNode);
                List<String> associationPropertyNames = getPropertyNamesForAssociation(type);
                if (associationPropertyNames == null) {
                    associationPropertyNames = getPropertyNamesForAssociation(classNode);
                }

                ClosureAndArguments closureAndArguments = new ClosureAndArguments(variableScope);
                BlockStatement currentBody = closureAndArguments.getCurrentBody();
                ArgumentListExpression arguments = closureAndArguments.getArguments();

                boolean hasNoProperties = associationPropertyNames.isEmpty();
                if (!hasNoProperties && !associationPropertyNames.contains(associationProperty)) {
                     sourceUnit.getErrorCollector().addError(new LocatedMessage("Cannot query property \""+associationProperty+"\" - no such property on class "+type.getName()+" exists.", Token.newString(propertyName, pe.getLineNumber(), pe.getColumnNumber()), sourceUnit));
                }
                ClassNode existing = this.currentClassNode;
                try {
                    this.currentClassNode = type;
                    if (functionName != null) {
                        handleFunctionCall(currentBody, operator, oppositeSide, functionName, new ConstantExpression(associationProperty));
                    }
                    else {
                        addCriteriaCallMethodExpression(currentBody, operator, oppositeSide, associationProperty, associationPropertyNames, hasNoProperties, variableScope);
                    }
                } finally {
                    this.currentClassNode = existing;
                }
                newCode.addStatement(new ExpressionStatement(new MethodCallExpression(DELEGATE_EXPRESSION, propertyName, arguments)));
            }
            else if (!variableScope.isReferencedLocalVariable(propertyName)) {
                sourceUnit.getErrorCollector().addError(new LocatedMessage("Cannot query property \""+propertyName+"\" - no such property on class "+this.currentClassNode.getName()+" exists.", Token.newString(propertyName, pe.getLineNumber(), pe.getColumnNumber()), sourceUnit));
            }
        }
    }

    private ClassNode getPropertyTypeFromGenerics(String propertyName, ClassNode classNode) {
        ClassNode type = getPropertyType(classNode, propertyName);
        if (type != null && !isDomainClass(type)) {
            ClassNode associationTypeFromGenerics = getAssociationTypeFromGenerics(type);
            if (associationTypeFromGenerics != null && isDomainClass(associationTypeFromGenerics)) {
                type = associationTypeFromGenerics;
            }
        }
        return type;
    }

    private void addCriteriaCallMethodExpression(BlockStatement newCode, String operator, Expression rightExpression, String propertyName, List<String> propertyNames, boolean addAll, VariableScope variableScope) {
        String methodToCall = OPERATOR_TO_CRITERIA_METHOD_MAP.get(operator);
        if (methodToCall == null) {
            sourceUnit.getErrorCollector().addError(new LocatedMessage("Unsupported operator ["+operator+"] used in query", Token.newString(rightExpression.getText(), rightExpression.getLineNumber(),rightExpression.getColumnNumber()), sourceUnit));
        }
        addCriteriaCall(newCode, operator, rightExpression, propertyName, propertyNames, addAll, methodToCall, variableScope);
    }

    private void addCriteriaCall(BlockStatement newCode, String operator, Expression rightExpression, String propertyName, List<String> propertyNames, boolean addAll, String methodToCall, VariableScope variableScope) {
        if (rightExpression instanceof VariableExpression) {
            String rightPropertyName = rightExpression.getText();
            if (!variableScope.isReferencedLocalVariable(rightPropertyName)) {
                if ((propertyNames.contains(rightPropertyName) || addAll) && PROPERTY_COMPARISON_OPERATOR_TO_CRITERIA_METHOD_MAP.containsKey(operator)) {
                    methodToCall = PROPERTY_COMPARISON_OPERATOR_TO_CRITERIA_METHOD_MAP.get(operator);
                    rightExpression = new ConstantExpression(rightPropertyName);
                }
            }
        } else if (rightExpression instanceof MethodCallExpression) {
            // potential aggregation
            MethodCallExpression aggregateMethodCall = (MethodCallExpression) rightExpression;
            String methodName = aggregateMethodCall.getMethodAsString();
            String aggregateFunctionName = AGGREGATE_FUNCTIONS.get(methodName);
            if ("of".equals(methodName) && aggregateMethodCall.getObjectExpression() instanceof MethodCallExpression) {
                ArgumentListExpression arguments = (ArgumentListExpression)aggregateMethodCall.getArguments();
                if (arguments.getExpressions().size() == 1 && arguments.getExpression(0) instanceof ClosureExpression) {
                    ClosureExpression ce = (ClosureExpression) arguments.getExpression(0);
                    transformClosureExpression(this.currentClassNode,ce);
                    aggregateMethodCall = (MethodCallExpression) aggregateMethodCall.getObjectExpression();

                    aggregateFunctionName = AGGREGATE_FUNCTIONS.get(aggregateMethodCall.getMethodAsString());
                    ArgumentListExpression aggregateMethodCallArguments = (ArgumentListExpression)aggregateMethodCall.getArguments();
                    if (aggregateFunctionName != null && aggregateMethodCallArguments.getExpressions().size() == 1) {
                        Expression expression = aggregateMethodCallArguments.getExpression(0);
                        String aggregatePropertyName = null;
                        if (expression instanceof VariableExpression || expression instanceof ConstantExpression) {
                            aggregatePropertyName = expression.getText();
                        }

                        boolean validProperty = aggregatePropertyName != null && propertyNames.contains(aggregatePropertyName);
                        if (validProperty) {
                            BlockStatement bs = (BlockStatement) ce.getCode();
                            addProjectionToCurrentBody(bs, aggregateFunctionName, aggregatePropertyName, variableScope);
                            rightExpression = new MethodCallExpression(new ConstructorCallExpression(DETACHED_CRITERIA_CLASS_NODE, new ArgumentListExpression(new ClassExpression(this.currentClassNode))), "build", new ArgumentListExpression(ce));
                        }
                    }
                }
            }
            else if (aggregateFunctionName != null) {
                Expression arguments = aggregateMethodCall.getArguments();
                if (arguments instanceof ArgumentListExpression) {
                    ArgumentListExpression argList = (ArgumentListExpression) arguments;
                    List<Expression> expressions = argList.getExpressions();
                    int argCount = expressions.size();
                    if (argCount == 1) {
                        Expression expression = expressions.get(0);
                        String aggregatePropertyName = null;
                        if (expression instanceof VariableExpression || expression instanceof ConstantExpression) {
                            aggregatePropertyName = expression.getText();
                        }
                        else {
                            sourceUnit.getErrorCollector().addError(new LocatedMessage("Cannot use aggregate function "+aggregateFunctionName+" on expressions \""+expression.getText()+"\".", Token.newString(propertyName,aggregateMethodCall.getLineNumber(), aggregateMethodCall.getColumnNumber()), sourceUnit));
                            return;
                        }

                        boolean validProperty = aggregatePropertyName != null && propertyNames.contains(aggregatePropertyName);
                        if (validProperty) {
                                ClosureAndArguments closureAndArguments = new ClosureAndArguments(variableScope);
                                BlockStatement currentBody = closureAndArguments.getCurrentBody();

                                addProjectionToCurrentBody(currentBody, aggregateFunctionName, aggregatePropertyName, variableScope);

                                rightExpression = closureAndArguments.getClosureExpression();

                                if ("property".equals(aggregateFunctionName)) {
                                    methodToCall = methodToCall + "All";
                                }
                        }
                        else {
                            sourceUnit.getErrorCollector().addError(new LocatedMessage("Cannot use aggregate function "+aggregateFunctionName+" on property \""+aggregatePropertyName+"\" - no such property on class "+this.currentClassNode.getName()+" exists.", Token.newString(propertyName,aggregateMethodCall.getLineNumber(), aggregateMethodCall.getColumnNumber()), sourceUnit));
                        }
                    }
                }
            }
            else if (isFunctionCall(aggregateMethodCall)) {
                // TODO: Allow function calls on right hand arguments
                ArgumentListExpression existingArgs = (ArgumentListExpression) aggregateMethodCall.getArguments();
                Expression propertyNameExpression = existingArgs.getExpression(0);
                sourceUnit.getErrorCollector().addError(new LocatedMessage("Function call "+aggregateFunctionName+" not allowed on property \""+propertyNameExpression.getText()+"\". Function calls can currently only be used on the left-hand side of expressions", Token.newString(propertyName,aggregateMethodCall.getLineNumber(), aggregateMethodCall.getColumnNumber()), sourceUnit));
                return ;
//
//                ArgumentListExpression newArgs = new ArgumentListExpression();
//                ArgumentListExpression constructorArgs = new ArgumentListExpression();
//                constructorArgs.addExpression(new ConstantExpression(methodName));
//                ClassNode criterionClassNode = OPERATOR_TO_PROPERTY_CRITERION_METHOD_MAP.get(operator);
//                if (criterionClassNode != null) {
//                    ArgumentListExpression criterionConstructorArguments = new ArgumentListExpression();
//                    if (!(propertyNameExpression instanceof ConstantExpression)) {
//                        propertyNameExpression = new ConstantExpression(propertyNameExpression.getText());
//                    }
//                    criterionConstructorArguments.addExpression(new ConstantExpression(propertyName));
//                    criterionConstructorArguments.addExpression(propertyNameExpression);
//
//                    constructorArgs.addExpression(new ConstructorCallExpression(criterionClassNode, criterionConstructorArguments));
//                    constructorArgs.addExpression(new ConstantExpression(true));
//                    ConstructorCallExpression constructorCallExpression = new ConstructorCallExpression(FUNCTION_CALL_CRITERION, constructorArgs);
//                    newArgs.addExpression(constructorCallExpression );
//
//                    newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "add", newArgs)));
//                }
//                else {
//                    sourceUnit.getErrorCollector().addError(new LocatedMessage("Function call "+aggregateFunctionName+" not allowed on property \""+propertyNameExpression.getText()+"\".", Token.newString(propertyName,aggregateMethodCall.getLineNumber(), aggregateMethodCall.getColumnNumber()), sourceUnit));
//                }
//                return;
            }
        } else {
            if ("like".equals(methodToCall) && rightExpression instanceof BitwiseNegationExpression) {
                methodToCall = "rlike";
                BitwiseNegationExpression bne = (BitwiseNegationExpression) rightExpression;
                rightExpression = bne.getExpression();
            }
            else if ("inList".equals(methodToCall) && rightExpression instanceof RangeExpression) {
                methodToCall = "between";
                RangeExpression re = (RangeExpression) rightExpression;
                ArgumentListExpression betweenArgs = new ArgumentListExpression();
                betweenArgs.addExpression(new ConstantExpression(propertyName))
                            .addExpression(re.getFrom())
                            .addExpression(re.getTo());
                rightExpression = betweenArgs;
            }
        }
        ArgumentListExpression arguments;

        if (rightExpression instanceof ArgumentListExpression) {
            arguments = (ArgumentListExpression) rightExpression;
        }
        else if (rightExpression instanceof ConstantExpression) {
            ConstantExpression constant = (ConstantExpression) rightExpression;
            if (constant.getValue() == null) {
                boolean singleArg = false;
                if (operator.equals(EQUALS_OPERATOR)) {
                    singleArg = true;
                    methodToCall = IS_NULL_CRITERION;
                }
                else if (operator.equals("!=")) {
                    singleArg = true;
                    methodToCall = "isNotNull";
                }

                arguments = new ArgumentListExpression();
                arguments.addExpression(new ConstantExpression(propertyName));
                if (!singleArg) {
                    arguments.addExpression(rightExpression);
                }
            }
            else {
                arguments = new ArgumentListExpression();
                arguments.addExpression(new ConstantExpression(propertyName))
                        .addExpression(rightExpression);
            }
        }
        else {
            arguments = new ArgumentListExpression();
            arguments.addExpression(new ConstantExpression(propertyName))
                    .addExpression(rightExpression);
        }
        newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, methodToCall, arguments)));
    }

    private void addProjectionToCurrentBody(BlockStatement currentBody, String functionName, String aggregatePropertyName, VariableScope variableScope) {
        ClosureAndArguments projectionsBody = new ClosureAndArguments(variableScope);
        ArgumentListExpression aggregateArgs = new ArgumentListExpression();
        aggregateArgs.addExpression(new ConstantExpression(aggregatePropertyName));
        projectionsBody.getCurrentBody().addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, functionName, aggregateArgs)));
        currentBody.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "projections", projectionsBody.getArguments())));
    }

    protected boolean isDomainClass(ClassNode classNode) {
        if (classNode == null) return false;
        String filePath = classNode.getModule() != null ? classNode.getModule().getDescription() : null;
        if (filePath != null) {
            try {
                if (GrailsResourceUtils.isDomainClass(new File(filePath).toURI().toURL())) {
                    return true;
                }
            } catch (MalformedURLException e) {
                // ignore
            }
        }
        List<AnnotationNode> annotations = classNode.getAnnotations();
        if (annotations != null && !annotations.isEmpty()) {
            for (AnnotationNode annotation : annotations) {
                String className = annotation.getClassNode().getName();
                if (Entity.class.getName().equals(className)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected SourceUnit getSourceUnit() {
        return this.sourceUnit;
    }

    private boolean isGetter(String methodName, MethodNode declaredMethod) {
        return declaredMethod.getParameters().length == 0 && GrailsClassUtils.isGetter(methodName, EMPTY_JAVA_CLASS_ARRAY);
    }

    private class ClosureAndArguments {
        private BlockStatement currentBody;
        private ArgumentListExpression arguments;
        private ClosureExpression closureExpression;
        private VariableScope variableScope;

        private ClosureAndArguments(VariableScope variableScope) {
            this.variableScope = variableScope;
            build();
        }

        public BlockStatement getCurrentBody() {
            return currentBody;
        }

        public ArgumentListExpression getArguments() {
            return arguments;
        }

        private ClosureAndArguments build() {
            currentBody = new BlockStatement();
            closureExpression = new ClosureExpression(new Parameter[0], currentBody);
            closureExpression.setVariableScope(variableScope);
            closureExpression.setCode(currentBody);

            arguments = new ArgumentListExpression();
            arguments.addExpression(closureExpression);
            return this;
        }

        public ClosureExpression getClosureExpression() {
            return closureExpression;
        }
    }

    @SuppressWarnings("rawtypes")
    private static <K, V> Map newMap(Object... keysAndValues) {
        if (keysAndValues == null) {
            return Collections.emptyMap();
        }
        if (keysAndValues.length % 2 == 1) {
            throw new IllegalArgumentException("Must have an even number of keys and values");
        }

        Map<K, V> map = new HashMap<K, V>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((K)keysAndValues[i], (V)keysAndValues[i + 1]);
        }
        return map;
    }

    private static <T> Set<T> newSet(T... values) {
        if (values == null) {
            return Collections.emptySet();
        }

        return new HashSet<T>(Arrays.asList(values));
    }
}
