package org.grails.datastore.gorm.cassandra;

import grails.gorm.CassandraEntity;
import grails.persistence.Entity;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.springframework.cassandra.core.PrimaryKeyType;
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.mapping.Table;
import org.springframework.util.ClassUtils;

/**
 * A AST transformation that turns a GORM entity into a Spring Data Cassandra
 * entity.
 *
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class GormToCassandraTransform implements ASTTransformation {

    private static Log LOG = LogFactory.getLog(GormToCassandraTransform.class);
    private static final AnnotationNode ANNOTATION_ENTITY = new AnnotationNode(new ClassNode(Entity.class));

    private static final ClassNode MY_TYPE = new ClassNode(CassandraEntity.class);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    public static final String ERRORS = "errors";

    public void visit(ASTNode[] astNodes, SourceUnit source) {
        if (!(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }

        AnnotatedNode parent = (AnnotatedNode) astNodes[1];
        AnnotationNode node = (AnnotationNode) astNodes[0];
        if (!MY_TYPE.equals(node.getClassNode()) || !(parent instanceof ClassNode)) {
            return;
        }

        ClassNode cNode = (ClassNode) parent;
        String cName = cNode.getName();
        if (cNode.isInterface()) {
            throw new RuntimeException("Error processing interface '" + cName + "'. " + MY_TYPE_NAME + " not allowed for interfaces.");
        }

        try {
            transformEntity(source, cNode);
        } catch (Exception e) {
            String message = "Error occured transfoming GORM entity to Cassandra entity: " + ExceptionUtils.getFullStackTrace(e);            
            LOG.error(message, e);
            source.getErrorCollector().addFatalError(new SimpleMessage(message, source));
        }
    }

    public static void transformEntity(SourceUnit source, ClassNode classNode) {
       
        classNode.addAnnotation(ANNOTATION_ENTITY);

        PropertyNode mappingNode = classNode.getProperty(GrailsDomainClassProperty.MAPPING);
        Map<String, Map<String, ?>> propertyMappings = new HashMap<String, Map<String, ?>>();
        if (mappingNode != null && mappingNode.isStatic()) {
            populateConfigurationMapFromClosureExpression(classNode, mappingNode, propertyMappings);
        }

        final PropertyNode errorsProperty = classNode.getProperty("errors");
        if (errorsProperty == null) {
            if (ClassUtils.isPresent("org.codehaus.groovy.grails.compiler.injection.ASTValidationErrorsHelper", Thread.currentThread().getContextClassLoader())) {
                // addErrorsProperty(classNode);
            }
        }

        // setup properties with PrimaryKeyColumn annotation
        String primaryKeyPropertyName = GrailsDomainClassProperty.IDENTITY;
        String generationType = "auto";
        for (Entry<String, Map<String, ?>> mappingEntry : propertyMappings.entrySet()) {
            String propertyName = mappingEntry.getKey();
            final Map<String, ?> propertyConfig = mappingEntry.getValue();
            final Map primaryKeyConfig;
            String columnName = null;
            Integer ordinal = null;
            PrimaryKeyType primaryKeyType = PrimaryKeyType.PARTITIONED;
            boolean annotatePrimaryKey = false;
            if (propertyConfig.containsKey("name") && propertyName.equals(GrailsDomainClassProperty.IDENTITY)) {
                annotatePrimaryKey = true;
                propertyName = propertyConfig.get("name").toString();
                primaryKeyPropertyName = propertyName;
            }
            if (propertyConfig.containsKey("column")) {
                columnName = (String) propertyConfig.get("column");
            }
            if (propertyConfig.containsKey("primaryKey")) {
                annotatePrimaryKey = true;
                primaryKeyConfig = (Map) propertyConfig.get("primaryKey");
                if (primaryKeyConfig.containsKey("ordinal")) {
                    ordinal = NumberUtils.toInt("" + primaryKeyConfig.get("ordinal"));
                }
                if (primaryKeyConfig.containsKey("type")) {
                    primaryKeyType = EnumUtils.getEnum(PrimaryKeyType.class, primaryKeyConfig.get("type").toString().toUpperCase());
                }
                primaryKeyPropertyName = propertyName;
            }
            if (propertyConfig.containsKey("generator")) {
                String generatorName = propertyConfig.get("generator").toString();
                if ("assigned".equals(generatorName)) {
                    generationType = null;
                }
            }
            if (annotatePrimaryKey) {
                AnnotationNode primaryKeyAnnotation = new AnnotationNode(new ClassNode(PrimaryKeyColumn.class));
                if (columnName != null) {
                    primaryKeyAnnotation.addMember("name", new ConstantExpression(columnName));
                }
                if (ordinal != null) {
                    primaryKeyAnnotation.addMember("ordinal", new ConstantExpression(ordinal));
                }
                if (primaryKeyType != null) {
                    primaryKeyAnnotation.addMember("type", new PropertyExpression(new ClassExpression(new ClassNode(PrimaryKeyType.class)), new ConstantExpression(primaryKeyType)));
                }
                annotateProperty(classNode, propertyName, primaryKeyAnnotation);
            }
        }

        injectIdProperty(classNode, primaryKeyPropertyName);

        PropertyNode primaryKeyProperty = classNode.getProperty(primaryKeyPropertyName);

        // annotate existing or newly created id field if not already present
        AnnotationNode primaryKeyAnnotation = new AnnotationNode(new ClassNode(PrimaryKeyColumn.class));
        primaryKeyAnnotation.addMember("type", new PropertyExpression(new ClassExpression(new ClassNode(PrimaryKeyType.class)), new ConstantExpression(PrimaryKeyType.PARTITIONED)));
        annotateProperty(classNode, primaryKeyProperty.getName(), primaryKeyAnnotation);

        // add table annotation if required
        annotateIfNecessary(classNode, Table.class);
    }

    private static void injectIdProperty(ClassNode classNode, String primaryKeyPropertyName) {
        final boolean hasId = GrailsASTUtils.hasOrInheritsProperty(classNode, primaryKeyPropertyName);

        if (!hasId) {
            // inject into furthest relative
            ClassNode parent = GrailsASTUtils.getFurthestUnresolvedParent(classNode);

            parent.addProperty(GrailsDomainClassProperty.IDENTITY, Modifier.PUBLIC, new ClassNode(UUID.class), null, null, null);
        }
    }

    private static String addErrorsScript = null;

    private static void addErrorsProperty(ClassNode classNode) {
        // Horrible to have to do this, but only way to support both Grails
        // 1.3.7 and Grails 2.0
        if (addErrorsScript == null) {
            URL resource = GormToCassandraTransform.class.getResource("/org/grails/datastore/gorm/cassandra/AddErrors.groovy");
            try {
                if (resource != null) {
                    addErrorsScript = DefaultGroovyMethods.getText(resource);
                }
            } catch (IOException e) {
                // ignore
            }
        }

        if (addErrorsScript != null) {
            Binding b = new Binding();
            b.setVariable("classNode", classNode);
            new GroovyShell(b).evaluate(addErrorsScript);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void populateConfigurationMapFromClosureExpression(ClassNode classNode, PropertyNode mappingNode, Map propertyMappings) {
        ClosureExpression ce = (ClosureExpression) mappingNode.getInitialExpression();
        final Statement code = ce.getCode();
        if (!(code instanceof BlockStatement)) {
            return;
        }
        final List<Statement> statements = ((BlockStatement) code).getStatements();
        for (Statement statement : statements) {
            if (!(statement instanceof ExpressionStatement)) {
                continue;
            }
            ExpressionStatement es = (ExpressionStatement) statement;
            final Expression expression = es.getExpression();
            if (!(expression instanceof MethodCallExpression)) {
                continue;
            }
            MethodCallExpression mce = (MethodCallExpression) expression;
            final String methodName = mce.getMethodAsString();
            Map propertyMapping = new HashMap();
            propertyMappings.put(methodName, propertyMapping);

            final Expression arguments = mce.getArguments();
            if (arguments instanceof ArgumentListExpression) {
                if (methodName.equals("table")) {
                    ArgumentListExpression ale = (ArgumentListExpression) arguments;
                    final List<Expression> expressions = ale.getExpressions();
                    if (!expressions.isEmpty()) {
                        final String tableName = expressions.get(0).getText();
                        final AnnotationNode tableAnnotation = new AnnotationNode(new ClassNode(Table.class));
                        tableAnnotation.addMember("value", new ConstantExpression(tableName));
                        classNode.addAnnotation(tableAnnotation);
                    }
                }
            } else if (arguments instanceof TupleExpression) {
                final List<Expression> tupleExpressions = ((TupleExpression) arguments).getExpressions();
                for (Expression te : tupleExpressions) {
                    if (!(te instanceof NamedArgumentListExpression)) {
                        continue;
                    }

                    NamedArgumentListExpression nale = (NamedArgumentListExpression) te;
                    for (MapEntryExpression mee : nale.getMapEntryExpressions()) {
                        String settingName = mee.getKeyExpression().getText();

                        final Expression valueExpression = mee.getValueExpression();
                        if (valueExpression instanceof ConstantExpression) {
                            if (valueExpression instanceof BooleanExpression) {
                                propertyMapping.put(settingName, Boolean.valueOf(valueExpression.getText()));
                            } else {
                                propertyMapping.put(settingName, valueExpression.getText());
                            }
                        } else if (valueExpression instanceof MapExpression) {
                            final Map<String, String> map = new LinkedHashMap<String, String>();
                            propertyMapping.put(settingName, map);
                            for (MapEntryExpression mee2 : ((MapExpression) valueExpression).getMapEntryExpressions()) {
                                map.put(mee2.getKeyExpression().getText(), mee2.getValueExpression().getText());
                            }
                        }
                    }
                }
            }
        }
    }

    private static Map<String, String> lookupStringToStringMap(ClassNode classNode, String mapName) {

        final PropertyNode mapProperty = classNode.getProperty(mapName);
        if (mapProperty == null) {
            return Collections.emptyMap();
        }

        final Expression initialExpression = mapProperty.getInitialExpression();
        if (!(initialExpression instanceof MapExpression)) {
            return Collections.emptyMap();
        }

        Map<String, String> stringToClassNodeMap = new HashMap<String, String>();
        MapExpression mapExpr = (MapExpression) initialExpression;
        final List<MapEntryExpression> mapEntryExpressions = mapExpr.getMapEntryExpressions();
        for (MapEntryExpression mapEntryExpression : mapEntryExpressions) {
            final Expression keyExpression = mapEntryExpression.getKeyExpression();
            if (!(keyExpression instanceof ConstantExpression)) {
                continue;
            }
            ConstantExpression ce = (ConstantExpression) keyExpression;
            String propertyName = ce.getValue().toString();
            final Expression valueExpression = mapEntryExpression.getValueExpression();
            if (valueExpression instanceof ConstantExpression) {
                stringToClassNodeMap.put(propertyName, ((ConstantExpression) valueExpression).getValue().toString());
            }
        }
        return stringToClassNodeMap;
    }

    private static Map<String, ClassNode> lookupStringToClassNodeMap(ClassNode classNode, String mapName) {

        if (classNode == null) {
            return Collections.emptyMap();
        }

        final PropertyNode mapProperty = classNode.getProperty(mapName);
        if (mapProperty == null) {
            return Collections.emptyMap();
        }

        final Expression initialExpression = mapProperty.getInitialExpression();
        if (!(initialExpression instanceof MapExpression)) {
            return Collections.emptyMap();
        }

        Map<String, ClassNode> stringToClassNodeMap = new HashMap<String, ClassNode>();
        MapExpression mapExpr = (MapExpression) initialExpression;
        final List<MapEntryExpression> mapEntryExpressions = mapExpr.getMapEntryExpressions();
        for (MapEntryExpression mapEntryExpression : mapEntryExpressions) {
            final Expression keyExpression = mapEntryExpression.getKeyExpression();
            if (!(keyExpression instanceof ConstantExpression)) {
                continue;
            }
            ConstantExpression ce = (ConstantExpression) keyExpression;
            String propertyName = ce.getValue().toString();
            final Expression valueExpression = mapEntryExpression.getValueExpression();
            if (valueExpression instanceof ClassExpression) {
                ClassExpression clsExpr = (ClassExpression) valueExpression;
                stringToClassNodeMap.put(propertyName, clsExpr.getType());
            }
        }
        return stringToClassNodeMap;
    }

    private static void annotateIfNecessary(ClassNode classNode, Class<?> annotationClass) {
        ClassNode annotationClassNode = new ClassNode(annotationClass);

        final List<AnnotationNode> annotations = classNode.getAnnotations(annotationClassNode);
        if (annotations == null || annotations.isEmpty()) {
            classNode.addAnnotation(new AnnotationNode(annotationClassNode));
        }
    }

    protected static void annotateAllProperties(ClassNode classNode, List<String> propertyNames, final Class<?> annotation) {
        final AnnotationNode annotationNode = new AnnotationNode(new ClassNode(annotation));
        annotateAllProperties(classNode, propertyNames, annotationNode);
    }

    protected static void annotateAllProperties(ClassNode classNode, Collection<String> propertyNames, final AnnotationNode annotationNode) {
        for (String propertyName : propertyNames) {
            annotateProperty(classNode, propertyName, annotationNode);
        }
    }

    protected static void annotateProperty(ClassNode classNode, String propertyName, Class<?> annotation) {
        annotateProperty(classNode, propertyName, new AnnotationNode(new ClassNode(annotation)));
    }

    protected static void annotateProperty(ClassNode classNode, String propertyName, final AnnotationNode annotationNode) {
        final PropertyNode prop = classNode.getProperty(propertyName);
        if (prop == null) {
            return;
        }

        final FieldNode fieldNode = prop.getField();
        if (fieldNode == null) {
            return;
        }

        final List<AnnotationNode> annotations = fieldNode.getAnnotations(annotationNode.getClassNode());
        if (annotations == null || annotations.isEmpty()) {
            fieldNode.addAnnotation(annotationNode);
        }
    }

    protected static void populateConstantList(List<String> theList, final PropertyNode theProperty) {
        if (theProperty == null) {
            return;
        }

        final Expression initialExpression = theProperty.getInitialExpression();
        if (initialExpression instanceof ListExpression) {
            ListExpression listExpression = (ListExpression) initialExpression;
            final List<Expression> entries = listExpression.getExpressions();
            for (Expression expression : entries) {
                if (expression instanceof ConstantExpression) {
                    addConstantExpressionToList(theList, expression);
                }
            }
        } else if (initialExpression instanceof ConstantExpression) {
            addConstantExpressionToList(theList, initialExpression);
        }
    }

    protected static void addConstantExpressionToList(List<String> theList, Expression expression) {
        final Object val = ((ConstantExpression) expression).getValue();
        if (val != null) {
            theList.add(val.toString());
        }
    }
}
