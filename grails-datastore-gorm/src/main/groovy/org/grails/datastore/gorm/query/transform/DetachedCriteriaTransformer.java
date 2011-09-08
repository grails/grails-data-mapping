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
import org.codehaus.groovy.control.messages.LocatedMessage;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.syntax.Token;

import java.util.*;


/**
 * ClassCodeVisitorSupport that transforms where methods into detached criteria queries
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class DetachedCriteriaTransformer extends ClassCodeVisitorSupport {

    private static final Class<?>[] EMPTY_JAVA_CLASS_ARRAY = {};
    private static final VariableExpression THIS_EXPRESSION = new VariableExpression("this");
    public static final String AND_OPERATOR = "&";
    public static final String OR_OPERATOR = "|";
    public static final ClassNode DETACHED_CRITERIA_CLASS_NODE = new ClassNode(DetachedCriteria.class);
    public static final HashSet<String> CANDIDATE_METHODS_WHERE_ONLY = new HashSet<String>() {{
        add("where");
    }};

    private SourceUnit sourceUnit;
    private static final Set<String> CANDIDATE_METHODS = new HashSet<String>() {{
        add("where");
        add("findAll");
        add("find");
    }};
    private static final Map<String, String> OPERATOR_TO_CRITERIA_METHOD_MAP = new HashMap<String, String>() {{
        put("==", "eq");
        put("!=", "ne");
        put(">", "gt");
        put("<", "lt");
        put(">=", "ge");
        put("<=", "le");
        put("==~", "like");
        put("in", "inList");

    }};
    private static final Map<String, String> PROPERTY_COMPARISON_OPERATOR_TO_CRITERIA_METHOD_MAP = new HashMap<String, String>() {{
        put("==", "eqProperty");
        put("!=", "neProperty");
        put(">", "gtProperty");
        put("<", "ltProperty");
        put(">=", "geProperty");
        put("<=", "leProperty");

    }};

    private Map<String, ClassNode> detachedCriteriaVariables = new HashMap<String, ClassNode>();

    DetachedCriteriaTransformer(SourceUnit sourceUnit) {
        this.sourceUnit = sourceUnit;
    }


    @Override
    public void visitClass(ClassNode node) {
        try {
            super.visitClass(node);
        } finally {
            detachedCriteriaVariables.clear();
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
        if(initializationExpression instanceof MethodCallExpression) {
            MethodCallExpression call = (MethodCallExpression) initializationExpression;
            Expression objectExpression = call.getObjectExpression();
            Expression method = call.getMethod();
            Expression arguments = call.getArguments();
            if(isCandidateMethod(method.getText(), arguments, CANDIDATE_METHODS_WHERE_ONLY)) {
                ClassNode classNode = new ClassNode(DetachedCriteria.class);
                ClassNode targetType = objectExpression.getType();
                if(isDomainClass(targetType)) {
                    classNode.setGenericsTypes(new GenericsType[]{new GenericsType(targetType)});

                    expression.getVariableExpression().setType(classNode);
                    String variableName = expression.getVariableExpression().getName();
                    expression.setLeftExpression(new VariableExpression(variableName, classNode));
                    detachedCriteriaVariables.put(variableName, targetType);
                }
            }

        }
        else {
            ClosureExpression newClosureExpression = handleDetachedCriteriaCast(initializationExpression);
            if (newClosureExpression != null) {
                expression.setRightExpression(newClosureExpression);
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
        if (isCandidateMethodCallForTransform(objectExpression, method, arguments)) {
            ClassExpression ce = (ClassExpression) objectExpression;
            ClassNode classNode = ce.getType();
            visitMethodCall(classNode, (ArgumentListExpression) arguments);
        }
        else if(objectExpression instanceof VariableExpression) {
            VariableExpression var = (VariableExpression) objectExpression;
            String varName = var.getName();
            ClassNode varType = detachedCriteriaVariables.get(varName);
            if(varType != null && isCandidateWhereMethod(method, arguments)) {
                visitMethodCall(varType, (ArgumentListExpression) arguments);
            }
        }
        super.visitMethodCallExpression(call);
    }

    private boolean isCandidateMethodCallForTransform(Expression objectExpression, Expression method, Expression arguments) {
        return (objectExpression instanceof ClassExpression) && isCandidateWhereMethod(method, arguments);
    }

    private void visitMethodCall(ClassNode classNode, ArgumentListExpression arguments) {
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
        if(isCandidateWhereMethod(method,arguments)) {
            ClassNode classNode = call.getOwnerType();
            visitMethodCall(classNode, (ArgumentListExpression) arguments);
        }
        super.visitStaticMethodCallExpression(call);
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
        boolean addAll = false;

        if (code instanceof BlockStatement) {
            BlockStatement bs = (BlockStatement) code;

            addBlockStatementToNewQuery(bs, newCode, addAll, propertyNames);
        }

        closureExpression.setCode(newCode);
    }

    private void addBlockStatementToNewQuery(BlockStatement blockStatement, BlockStatement newCode, boolean addAll, List<String> propertyNames) {
        List<Statement> statements = blockStatement.getStatements();
        for (Statement statement : statements) {
            if (statement instanceof ExpressionStatement) {
                ExpressionStatement es = (ExpressionStatement) statement;

                Expression expression = es.getExpression();
                if (expression instanceof BinaryExpression) {
                    BinaryExpression be = (BinaryExpression) expression;
                    addBinaryExpressionToNewBody(propertyNames, newCode, be, addAll);
                } else if (expression instanceof NotExpression) {
                    NotExpression not = (NotExpression) expression;

                    handleNegation(propertyNames, newCode, not);
                } else if(expression instanceof MethodCallExpression) {
                    MethodCallExpression methodCall = (MethodCallExpression) expression;

                    handleAssociationMethodCallExpression(newCode, methodCall, propertyNames);
                }
                else {
                    sourceUnit.getErrorCollector().addError(new LocatedMessage("Only binary expressions are allowed in queries.", Token.newString(expression.getText(),expression.getLineNumber(), expression.getColumnNumber()), sourceUnit));
                }

            }
        }
    }

    private void handleAssociationMethodCallExpression(BlockStatement newCode, MethodCallExpression methodCall, List<String> propertyNames) {
        Expression method = methodCall.getMethod();
        String methodName = method.getText();
        ArgumentListExpression arguments = methodCall.getArguments() instanceof ArgumentListExpression ? (ArgumentListExpression) methodCall.getArguments() : null;

        if(isAssociationMethodCall(propertyNames, methodName, arguments)) {
            ClosureAndArguments closureAndArguments = new ClosureAndArguments();
            ClosureExpression associationQuery = (ClosureExpression) arguments.getExpression(0);
            BlockStatement currentBody = closureAndArguments.getCurrentBody();
            ArgumentListExpression argList = closureAndArguments.getArguments();
            newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, methodName, argList)));
            Statement associationCode = associationQuery.getCode();
            if(associationCode instanceof BlockStatement) {
                 addBlockStatementToNewQuery((BlockStatement) associationCode,currentBody, true, propertyNames);
            }
        }
        else {
            sourceUnit.getErrorCollector().addError(new LocatedMessage("Method call ["+methodName+"] is invalid. Only binary expressions are allowed in queries.", Token.newString(methodName,methodCall.getLineNumber(), methodCall.getColumnNumber()), sourceUnit));
        }
    }

    private boolean isAssociationMethodCall(List<String> propertyNames, String methodName, ArgumentListExpression arguments) {
        return propertyNames.contains(methodName) && arguments != null && arguments.getExpressions().size()  == 1 && (arguments.getExpression(0) instanceof ClosureExpression);
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
        else {
            sourceUnit.getErrorCollector().addError(new LocatedMessage("You can only negate a binary expressions in queries.", Token.newString(not.getText(),not.getLineNumber(), not.getColumnNumber()), sourceUnit));
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
                else {
                    sourceUnit.getErrorCollector().addError(new LocatedMessage("Unsupported operator ["+operator+"] used in query", operation, sourceUnit));
                }
            }
            else {
                sourceUnit.getErrorCollector().addError(new LocatedMessage("Cannot query on invalid property ["+propertyName+"] ", Token.newString(propertyName,leftExpression.getLineNumber(), leftExpression.getColumnNumber()), sourceUnit));
            }
        }   else  {
            String methodNameToCall = null;
            if (operator.contains(AND_OPERATOR)) {
                methodNameToCall = "and";
            } else if (operator.contains(OR_OPERATOR)) {
                methodNameToCall = "or";
            }
            ArgumentListExpression arguments = new ArgumentListExpression();
            BlockStatement currentBody = new BlockStatement();
            handleBinaryExpressionSide(leftExpression,rightExpression, operator, currentBody, addAll, propertyNames);
            handleBinaryExpressionSide(rightExpression, rightExpression, operator,currentBody, addAll, propertyNames);

            ClosureExpression newClosureExpression = new ClosureExpression(new Parameter[0], currentBody);
            newClosureExpression.setVariableScope(new VariableScope());
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

    private void handleBinaryExpressionSide(Expression expressionSide, Expression oppositeSide, String operator, BlockStatement newCode, boolean addAll, List<String> propertyNames) {
        if (expressionSide instanceof BinaryExpression) {
            addBinaryExpressionToNewBody(propertyNames, newCode, (BinaryExpression) expressionSide, addAll);
        } else if(expressionSide instanceof NotExpression) {
            handleNegation(propertyNames, newCode, (NotExpression) expressionSide);
        }
        else if(expressionSide instanceof MethodCallExpression) {
            MethodCallExpression methodCallExpression = (MethodCallExpression) expressionSide;
            handleAssociationMethodCallExpression(newCode, methodCallExpression, propertyNames);
        }
        else if (expressionSide instanceof PropertyExpression) {
            PropertyExpression pe = (PropertyExpression) expressionSide;
            Expression objectExpression = pe.getObjectExpression();
            if (objectExpression instanceof VariableExpression) {
                String propertyName = objectExpression.getText();
                if (propertyNames.contains(propertyName)) {
                    String associationProperty = pe.getPropertyAsString();

                    ClosureAndArguments closureAndArguments = new ClosureAndArguments();
                    BlockStatement currentBody = closureAndArguments.getCurrentBody();
                    ArgumentListExpression arguments = closureAndArguments.getArguments();

                    addCriteriaCallMethodExpression(currentBody, operator, oppositeSide, associationProperty, propertyNames, true);
                    newCode.addStatement(new ExpressionStatement(new MethodCallExpression(THIS_EXPRESSION, propertyName, arguments)));
                }
            }
        }
        else {
            // TODO: compilation error
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

    private class ClosureAndArguments {
        private BlockStatement currentBody;
        private ArgumentListExpression arguments;

        private ClosureAndArguments() {
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
            ClosureExpression newClosureExpression = new ClosureExpression(new Parameter[0], currentBody);
            newClosureExpression.setVariableScope(new VariableScope());
            newClosureExpression.setCode(currentBody);

            arguments = new ArgumentListExpression();
            arguments.addExpression(newClosureExpression);
            return this;
        }
    }
}