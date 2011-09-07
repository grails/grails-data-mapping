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
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.syntax.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * ClassCodeVisitorSupport that transforms where methods into detached criteria queries
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class DetachedCriteriaTransformer extends ClassCodeVisitorSupport {

    private static final Class<?>[] EMPTY_JAVA_CLASS_ARRAY = {};
    private static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");

    private SourceUnit sourceUnit;
    private final Map<String, String> OPERATOR_TO_CRITERIA_METHOD_MAP = new HashMap<String, String>() {{
        put("==", "eq");
        put("!=", "ne");
        put(">", "gt");
        put("<", "lt");
        put(">=", "ge");
        put("<=", "le");
        put("==~", "like");
        put("in", "inList");

    }};
    private final Map<String, String> PROPERTY_COMPARISON_OPERATOR_TO_CRITERIA_METHOD_MAP = new HashMap<String, String>() {{
        put("==", "eqProperty");
        put("!=", "neProperty");
        put(">", "gtProperty");
        put("<", "ltProperty");
        put(">=", "geProperty");
        put("<=", "leProperty");

    }};

    DetachedCriteriaTransformer(SourceUnit sourceUnit) {
        this.sourceUnit = sourceUnit;
    }

    @Override
    public void visitField(FieldNode node) {
        Expression initialExpression = node.getInitialExpression();
        ClosureExpression newClosureExpression = handleDetachedCriteriaCast(initialExpression);

        if (newClosureExpression != null) {
            node.setInitialValueExpression(newClosureExpression);
        }

        super.visitField(node);
    }

    @Override
    public void visitDeclarationExpression(DeclarationExpression expression) {
        Expression initializationExpression = expression.getRightExpression();
        ClosureExpression newClosureExpression = handleDetachedCriteriaCast(initializationExpression);

        if (newClosureExpression != null) {
            expression.setRightExpression(newClosureExpression);
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
        if ((objectExpression instanceof ClassExpression) && ((method instanceof ConstantExpression) && ((ConstantExpression) method).getValue().equals("where")) && (arguments instanceof ArgumentListExpression)) {
            ClassExpression ce = (ClassExpression) objectExpression;
            ClassNode classNode = ce.getType();
            if (isDomainClass(classNode)) {
                ArgumentListExpression argList = (ArgumentListExpression) arguments;
                if (argList.getExpressions().size() == 1) {
                    Expression expression = argList.getExpression(0);
                    if (expression instanceof ClosureExpression) {
                        ClosureExpression closureExpression = (ClosureExpression) expression;
                        transformClosureExpression(classNode, closureExpression);

                    }
                }
            }
        }
        super.visitMethodCallExpression(call);
    }

    protected void transformClosureExpression(ClassNode classNode, ClosureExpression closureExpression) {
        List<MethodNode> methods = classNode.getMethods();
        List<String> propertyNames = new ArrayList<String>();
        for (MethodNode method : methods) {
            if (!method.isAbstract() && !method.isStatic() && isGetter(method.getName(), method)) {
                propertyNames.add(GrailsClassUtils.getPropertyForGetter(method.getName()));
            }
        }
        Statement code = closureExpression.getCode();
        BlockStatement newCode = new BlockStatement();

        if (code instanceof BlockStatement) {
            BlockStatement bs = (BlockStatement) code;

            List<Statement> statements = bs.getStatements();
            for (Statement statement : statements) {
                if (statement instanceof ExpressionStatement) {
                    ExpressionStatement es = (ExpressionStatement) statement;

                    Expression expression = es.getExpression();
                    if (expression instanceof BinaryExpression) {
                        BinaryExpression be = (BinaryExpression) expression;
                        addBinaryExpressionToNewBody(propertyNames, newCode, be, false);
                    } else if (expression instanceof NotExpression) {
                        NotExpression not = (NotExpression) expression;

                        handleNegation(propertyNames, newCode, not);
                    }
                }
            }
        }

        closureExpression.setCode(newCode);
    }

    private void handleNegation(List<String> propertyNames, BlockStatement newCode, NotExpression not) {
        Expression subExpression = not.getExpression();
        if (subExpression instanceof BinaryExpression) {
            ArgumentListExpression arguments = new ArgumentListExpression();
            BlockStatement currentBody = new BlockStatement();
            ClosureExpression newClosureExpression = new ClosureExpression(new Parameter[0], currentBody);
            newClosureExpression.setVariableScope(new VariableScope());
            arguments.addExpression(newClosureExpression);
            addBinaryExpressionToNewBody(propertyNames, currentBody, (BinaryExpression) subExpression, false);

            newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, "not", arguments)));
        }
    }

    private void addBinaryExpressionToNewBody(List<String> propertyNames, BlockStatement newCode, BinaryExpression be, boolean addAll) {
        Token operation = be.getOperation();

        String operator = operation.getRootText();

        Expression leftExpression = be.getLeftExpression();
        Expression rightExpression = be.getRightExpression();
        if (leftExpression instanceof VariableExpression) {
            VariableExpression leftVariable = (VariableExpression) leftExpression;
            String propertyName = leftVariable.getText();
            if (propertyNames.contains(propertyName) || addAll) {
                if (OPERATOR_TO_CRITERIA_METHOD_MAP.containsKey(operator)) {
                    addCriteriaCallMethodExpression(newCode, operator, rightExpression, propertyName, propertyNames, addAll);
                }
            }
        } else if (((leftExpression instanceof BinaryExpression) || (leftExpression instanceof NotExpression)) && ((rightExpression instanceof BinaryExpression) || (rightExpression instanceof NotExpression))) {
            String methodNameToCall = null;
            if (operator.contains("&")) {
                methodNameToCall = "and";
            } else if (operator.contains("|")) {
                methodNameToCall = "or";
            }
            ArgumentListExpression arguments = new ArgumentListExpression();
            BlockStatement currentBody = new BlockStatement();
            if (leftExpression instanceof BinaryExpression) {
                addBinaryExpressionToNewBody(propertyNames, currentBody, (BinaryExpression) leftExpression, false);
            } else {
                handleNegation(propertyNames, currentBody, (NotExpression) leftExpression);
            }
            if (rightExpression instanceof BinaryExpression) {
                addBinaryExpressionToNewBody(propertyNames, currentBody, (BinaryExpression) rightExpression, false);
            } else {
                handleNegation(propertyNames, currentBody, (NotExpression) rightExpression);
            }
            ClosureExpression newClosureExpression = new ClosureExpression(new Parameter[0], currentBody);
            newClosureExpression.setVariableScope(new VariableScope());
            arguments.addExpression(newClosureExpression);
            if (methodNameToCall != null) {
                newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, methodNameToCall, arguments)));
            }
        } else if (leftExpression instanceof PropertyExpression) {
            PropertyExpression pe = (PropertyExpression) leftExpression;
            Expression objectExpression = pe.getObjectExpression();
            if (objectExpression instanceof VariableExpression) {
                String propertyName = objectExpression.getText();
                if (propertyNames.contains(propertyName)) {
                    String associationProperty = pe.getPropertyAsString();

                    BlockStatement currentBody = new BlockStatement();
                    ClosureExpression newClosureExpression = new ClosureExpression(new Parameter[0], currentBody);
                    newClosureExpression.setVariableScope(new VariableScope());
                    newClosureExpression.setCode(currentBody);
                    ArgumentListExpression arguments = new ArgumentListExpression();
                    arguments.addExpression(newClosureExpression);
                    addCriteriaCallMethodExpression(currentBody, operator, rightExpression, associationProperty, propertyNames, true);

                    newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, propertyName, arguments)));
                }
            }

        }
    }

    private void addCriteriaCallMethodExpression(BlockStatement newCode, String operator, Expression rightExpression, String propertyName, List<String> propertyNames, boolean addAll) {
        String methodToCall = OPERATOR_TO_CRITERIA_METHOD_MAP.get(operator);
        if (rightExpression instanceof VariableExpression) {
            String rightPropertyName = rightExpression.getText();
            if ((propertyNames.contains(rightPropertyName) || addAll) && PROPERTY_COMPARISON_OPERATOR_TO_CRITERIA_METHOD_MAP.containsKey(operator)) {
                methodToCall = PROPERTY_COMPARISON_OPERATOR_TO_CRITERIA_METHOD_MAP.get(operator);
                rightExpression = new ConstantExpression(rightPropertyName);
            }
        } else {
            if ("like".equals(methodToCall) && rightExpression instanceof BitwiseNegationExpression) {
                methodToCall = "rlike";
                BitwiseNegationExpression bne = (BitwiseNegationExpression) rightExpression;
                rightExpression = bne.getExpression();
            }
        }
        ArgumentListExpression arguments = new ArgumentListExpression();
        arguments.addExpression(new ConstantExpression(propertyName))
                .addExpression(rightExpression);
        newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, methodToCall, arguments)));
    }

    protected boolean isDomainClass(ClassNode classNode) {
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

}