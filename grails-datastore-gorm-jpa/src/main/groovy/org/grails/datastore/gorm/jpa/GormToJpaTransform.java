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
package org.grails.datastore.gorm.jpa;

import grails.gorm.JpaEntity;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
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
import org.codehaus.groovy.grails.compiler.injection.DefaultGrailsDomainClassInjector;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.grails.datastore.mapping.model.MappingFactory;
import org.springframework.util.ClassUtils;

/**
 * A AST transformation that turns a GORM entity into a JPA entity.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class GormToJpaTransform implements ASTTransformation {

    private static Log LOG = LogFactory.getLog(GormToJpaTransform.class);
    private static final AnnotationNode ANNOTATION_VERSION = new AnnotationNode(new ClassNode(Version.class));
    private static final AnnotationNode ANNOTATION_ID = new AnnotationNode(new ClassNode(Id.class));
    private static final AnnotationNode ANNOTATION_ENTITY = new AnnotationNode(new ClassNode(Entity.class));
    private static final AnnotationNode ANNOTATION_BASIC = new AnnotationNode(new ClassNode(Basic.class));

    private static final PropertyExpression EXPR_CASCADE_ALL = new PropertyExpression(
            new ClassExpression(new ClassNode(CascadeType.class)), "ALL");
    private static final PropertyExpression EXPR_CASCADE_PERSIST = new PropertyExpression(
            new ClassExpression(new ClassNode(CascadeType.class)), "PERSIST");

    private static final ClassNode MY_TYPE = new ClassNode(JpaEntity.class);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();

    @SuppressWarnings("serial")
    private static final Map<String, AnnotationNode> gormEventMethodToJpaAnnotation = new HashMap<String, AnnotationNode>() {{
        put("beforeInsert", new AnnotationNode(new ClassNode(PrePersist.class)));
        put("afterInsert", new AnnotationNode(new ClassNode(PostPersist.class)));
        put("beforeUpdate", new AnnotationNode(new ClassNode(PreUpdate.class)));
        put("afterUpdate", new AnnotationNode(new ClassNode(PostUpdate.class)));
        put("beforeDelete", new AnnotationNode(new ClassNode(PreRemove.class)));
        put("afterDelete", new AnnotationNode(new ClassNode(PostRemove.class)));
        put("onSave", new AnnotationNode(new ClassNode(PrePersist.class)));
        put("afterLoad", new AnnotationNode(new ClassNode(PostLoad.class)));
    }};
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
            throw new RuntimeException("Error processing interface '" + cName + "'. " +
                    MY_TYPE_NAME + " not allowed for interfaces.");
        }

        try {
            transformEntity(source, cNode);
        } catch (Exception e) {
            String message = "Error occured transfoming GORM entity to JPA entity: " + e.getMessage();
            LOG.error(message,e);
            source.getErrorCollector().addFatalError(new SimpleMessage(message, source));
        }
    }

    public static void transformEntity(SourceUnit source, ClassNode classNode) {

        // add the JPA @Entity annotation
        classNode.addAnnotation(ANNOTATION_ENTITY);
        final AnnotationNode entityListenersAnnotation = new AnnotationNode(
                new ClassNode(EntityListeners.class));
        entityListenersAnnotation.addMember("value", new ClassExpression(
                new ClassNode(EntityInterceptorInvokingEntityListener.class)));
        classNode.addAnnotation(entityListenersAnnotation);

        PropertyNode mappingNode = classNode.getProperty(GrailsDomainClassProperty.MAPPING);
        Map<String, Map<String, ?>> propertyMappings = new HashMap<String, Map<String, ?>>();
        if (mappingNode != null && mappingNode.isStatic()) {
            populateConfigurationMapFromClosureExpression(
                    classNode, mappingNode, propertyMappings);
        }

        // annotate the id property with @Id
        String idPropertyName = GrailsDomainClassProperty.IDENTITY;
        String generationType = GenerationType.AUTO.toString();

        final PropertyNode errorsProperty = classNode.getProperty("errors");
        if (errorsProperty == null) {
            if (ClassUtils.isPresent("org.codehaus.groovy.grails.compiler.injection.ASTValidationErrorsHelper", Thread.currentThread().getContextClassLoader())) {
                addErrorsProperty(classNode);
            }
        }

        if (propertyMappings.containsKey(GrailsDomainClassProperty.IDENTITY)) {
            final Map<String, ?> idConfig = propertyMappings.get(GrailsDomainClassProperty.IDENTITY);
            if (idConfig.containsKey("name")) {
                idPropertyName = idConfig.get("name").toString();
            }
            if (idConfig.containsKey("generator")) {
                String generatorName = idConfig.get("generator").toString();
                if ("assigned".equals(generatorName)) {
                    generationType = null;
                }
                else if ("sequence".equals(generatorName)) {
                    generationType = GenerationType.SEQUENCE.toString();
                }
                else if ("identity".equals(generatorName)) {
                    generationType = GenerationType.IDENTITY.toString();
                }
            }
        }

        PropertyNode idProperty = classNode.getProperty(idPropertyName);
        if (idProperty == null) {
            new DefaultGrailsDomainClassInjector().performInjectionOnAnnotatedEntity(classNode);
            idProperty = classNode.getProperty(GrailsDomainClassProperty.IDENTITY);
        }

        if (!idPropertyName.equals(GrailsDomainClassProperty.IDENTITY)) {
            PropertyNode toDiscard = classNode.getProperty(GrailsDomainClassProperty.IDENTITY);
            if (toDiscard != null && toDiscard.getType().equals("java.lang.Long")) {
                classNode.getProperties().remove(toDiscard);
            }
        }

        if (idProperty != null) {
            final FieldNode idField = idProperty.getField();

            idField.addAnnotation(ANNOTATION_ID);
            if (generationType != null) {
                final AnnotationNode generatedValueAnnotation = new AnnotationNode(new ClassNode(GeneratedValue.class));
                generatedValueAnnotation.addMember("strategy", new PropertyExpression(new ClassExpression(new ClassNode(GenerationType.class)), generationType));
                idField.addAnnotation(generatedValueAnnotation);
            }
        }

        // annotate the version property with @Version
        PropertyNode versionProperty = classNode.getProperty(GrailsDomainClassProperty.VERSION);
        if (versionProperty != null) {
            if (propertyMappings.containsKey(GrailsDomainClassProperty.VERSION)) {
                final Map<String, ?> versionSettings = propertyMappings.get(GrailsDomainClassProperty.VERSION);
                final Object enabledObject = versionSettings.get("enabled");
                if (enabledObject instanceof Boolean) {
                    if (((Boolean)enabledObject).booleanValue()) {
                        versionProperty.addAnnotation(ANNOTATION_VERSION);
                    }
                }
            }
            else {
                versionProperty.addAnnotation(ANNOTATION_VERSION);
            }
        }

        final List<MethodNode> methods = classNode.getMethods();
        for (MethodNode methodNode : methods) {
            if (methodNode.isStatic() || !methodNode.isPublic() || methodNode.isAbstract()) {
                continue;
            }

            final AnnotationNode annotationNode = gormEventMethodToJpaAnnotation.get(methodNode.getName());
            if (annotationNode == null) {
                continue;
            }

            //methodNode.setReturnType(new ClassNode(void.class));
            methodNode.addAnnotation(annotationNode);
        }

        Map<String, ClassNode> hasManyMap = lookupStringToClassNodeMap(classNode, GrailsDomainClassProperty.HAS_MANY);
        Map<String, ClassNode> hasOneMap = lookupStringToClassNodeMap(classNode, GrailsDomainClassProperty.HAS_ONE);
        Map<String, ClassNode> belongsToMap = lookupStringToClassNodeMap(classNode, GrailsDomainClassProperty.BELONGS_TO);
        Map<String, String> mappedByMap = lookupStringToStringMap(classNode, GrailsDomainClassProperty.MAPPED_BY);

        final List<PropertyNode> properties = classNode.getProperties();
        for (PropertyNode propertyNode : properties) {
            if (!propertyNode.isPublic() || propertyNode.isStatic()) {
                continue;
            }

            if (propertyNode == idProperty || propertyNode == versionProperty) {
                continue;
            }

            final String typeName = propertyNode.getType().getName();

            if (typeName.equals("java.util.Date") || typeName.equals("java.util.Calendar")) {
                AnnotationNode temporalAnnotation = new AnnotationNode(new ClassNode(Temporal.class));
                temporalAnnotation.addMember("value", new PropertyExpression(
                        new ClassExpression(new ClassNode(TemporalType.class)), "DATE"));
                propertyNode.getField().addAnnotation(temporalAnnotation);
            }
            else if (MappingFactory.isSimpleType(typeName)) {
                propertyNode.getField().addAnnotation(ANNOTATION_BASIC);
            }
            else {
                final String propertyName = propertyNode.getName();
                if (!belongsToMap.containsKey(propertyName) &&
                    !hasOneMap.containsKey(propertyName)&&
                    !hasManyMap.containsKey(propertyName)) {
                    handleToOne(classNode, belongsToMap, propertyName);
                }
            }
        }

        final PropertyNode transientsProp = classNode.getProperty(GrailsDomainClassProperty.TRANSIENT);
        List<String> propertyNameList = new ArrayList<String>();
        populateConstantList(propertyNameList, transientsProp);
        annotateAllProperties(classNode, propertyNameList, Transient.class);

        propertyNameList.clear();
        final PropertyNode embeddedProp = classNode.getProperty(GrailsDomainClassProperty.EMBEDDED);
        populateConstantList(propertyNameList, embeddedProp);
        annotateAllProperties(classNode, propertyNameList, Embedded.class);

        if (embeddedProp != null) {
            for (String propertyName : propertyNameList) {
                final PropertyNode property = classNode.getProperty(propertyName);
                if (property == null) {
                    continue;
                }

                ClassNode embeddedType = property.getField().getType();
                annotateIfNecessary(embeddedType, Embeddable.class);
            }
        }

        if (!belongsToMap.isEmpty()) {
            for (String propertyName : belongsToMap.keySet()) {
                handleToOne(classNode, belongsToMap, propertyName);
            }
        }

        if (!hasOneMap.isEmpty()) {
            for (String propertyName : hasOneMap.keySet()) {
                final AnnotationNode oneToOneAnnotation = new AnnotationNode(new ClassNode(OneToOne.class));
                oneToOneAnnotation.addMember("optional", ConstantExpression.FALSE);
                oneToOneAnnotation.addMember("cascade", EXPR_CASCADE_ALL);
                annotateProperty(classNode, propertyName, oneToOneAnnotation);
            }
        }

        if (!hasManyMap.isEmpty()) {
            for (String propertyName : hasManyMap.keySet()) {
                ClassNode associatedClass = hasManyMap.get(propertyName);
                final Map<String, ClassNode> inverseBelongsToMap = lookupStringToClassNodeMap(
                        associatedClass, GrailsDomainClassProperty.BELONGS_TO);
                final Map<String, ClassNode> inverseHasManyMap = lookupStringToClassNodeMap(
                        associatedClass, GrailsDomainClassProperty.HAS_MANY);

                final AnnotationNode oneToManyAnnotation = new AnnotationNode(
                        new ClassNode(OneToMany.class));
                oneToManyAnnotation.addMember("targetEntity", new ClassExpression(associatedClass));

                if (mappedByMap.containsKey(propertyName)) {
                    oneToManyAnnotation.addMember("mappedBy", new ConstantExpression(
                            mappedByMap.get(propertyName)));
                    oneToManyAnnotation.addMember("cascade", EXPR_CASCADE_PERSIST);
                    annotateProperty(classNode, propertyName, oneToManyAnnotation);
                }
                else {
                    if (inverseHasManyMap.containsValue(classNode)) {
                        // many-to-many association
                        List<ClassNode> belongsToList = getBelongsToList(classNode);

                        final AnnotationNode manyToManyAnnotation = new AnnotationNode(new ClassNode(ManyToMany.class));
                        manyToManyAnnotation.addMember("targetEntity", new ClassExpression(associatedClass));
                        if (belongsToList.contains(associatedClass)) {
                            for (String inversePropertyName : inverseHasManyMap.keySet()) {
                                if (classNode.equals(inverseHasManyMap.get(inversePropertyName))) {
                                    manyToManyAnnotation.addMember("mappedBy", new ConstantExpression(inversePropertyName));
                                }
                            }
                        }
                        else {
                            manyToManyAnnotation.addMember("cascade", EXPR_CASCADE_ALL);
                        }
                        annotateProperty(classNode, propertyName, manyToManyAnnotation);
                    }
                    // Try work out the other side of the association
                    else if (inverseBelongsToMap.containsValue(classNode)) {
                        for (String inversePropertyName : inverseBelongsToMap.keySet()) {
                            if (classNode.equals(inverseBelongsToMap.get(inversePropertyName))) {
                                oneToManyAnnotation.addMember("mappedBy", new ConstantExpression(inversePropertyName));
                                oneToManyAnnotation.addMember("cascade", EXPR_CASCADE_ALL);
                            }
                        }
                        annotateProperty(classNode, propertyName, oneToManyAnnotation);
                    }
                    else {
                        PropertyNode inverseClosestMatch = findClosestInverstTypeMatch(classNode, associatedClass);
                        if (inverseClosestMatch != null) {
                            oneToManyAnnotation.addMember("mappedBy", new ConstantExpression(inverseClosestMatch.getName()));
                        }
                        // unidrectional one-to-many
                        oneToManyAnnotation.addMember("cascade", EXPR_CASCADE_ALL);
                        annotateProperty(classNode, propertyName, oneToManyAnnotation);
                    }
                }
            }
        }
    }

    private static String addErrorsScript = null;
    private static void addErrorsProperty(ClassNode classNode) {
        // Horrible to have to do this, but only way to support both Grails 1.3.7 and Grails 2.0
        if (addErrorsScript == null) {
            URL resource = GormToJpaTransform.class.getResource("/org/grails/datastore/gorm/jpa/AddErrors.script");
            try {
                if (resource != null) {
                    addErrorsScript = DefaultGroovyMethods.getText(resource);
                }
            } catch (IOException e) {
                // ignore
            }
        }

        if (addErrorsScript != null)   {
            Binding b = new Binding();
            b.setVariable("classNode", classNode);
            new GroovyShell(b).evaluate(addErrorsScript);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void populateConfigurationMapFromClosureExpression(ClassNode classNode,
            PropertyNode mappingNode, Map propertyMappings) {
        ClosureExpression ce = (ClosureExpression) mappingNode.getInitialExpression();
        final Statement code = ce.getCode();
        if (!(code instanceof BlockStatement)) {
            return;
        }
        final List<Statement> statements = ((BlockStatement)code).getStatements();
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
                        tableAnnotation.addMember("name", new ConstantExpression(tableName));
                        classNode.addAnnotation(tableAnnotation);
                    }
                }
                else if (methodName.equals("version")) {
                    ArgumentListExpression ale = (ArgumentListExpression) arguments;
                    final List<Expression> expressions = ale.getExpressions();
                    if (!expressions.isEmpty()) {
                        final Expression expr = expressions.get(0);
                        if (expr instanceof BooleanExpression) {
                            propertyMapping.put("enabled", Boolean.valueOf(expr.getText()));
                        }
                    }
                }
            }
            else if (arguments instanceof TupleExpression) {
                final List<Expression> tupleExpressions = ((TupleExpression)arguments).getExpressions();
                for (Expression te : tupleExpressions) {
                    if (!(te instanceof NamedArgumentListExpression)) {
                        continue;
                    }

                    NamedArgumentListExpression nale = (NamedArgumentListExpression) te;
                    for (MapEntryExpression mee : nale.getMapEntryExpressions()) {
                        String settingName = mee.getKeyExpression().getText();

                        final Expression valueExpression = mee.getValueExpression();
                        if (!(valueExpression instanceof ConstantExpression)) {
                            continue;
                        }

                        if (valueExpression instanceof BooleanExpression) {
                            propertyMapping.put(settingName, Boolean.valueOf(valueExpression.getText()));
                        }
                        else {
                            propertyMapping.put(settingName, valueExpression.getText());
                        }
                    }
                }
            }
        }
    }

    private static PropertyNode findClosestInverstTypeMatch(
            ClassNode classNode, ClassNode associatedClass) {
        for (PropertyNode inverseProperty : associatedClass.getProperties()) {
            if (inverseProperty.isPublic() && inverseProperty.getType().equals(classNode)) {
                return inverseProperty;
            }
        }
        return null;
    }

    static void handleToOne(ClassNode classNode,
            Map<String, ClassNode> belongsToMap, String propertyName) {
        ClassNode associatedClass = belongsToMap.get(propertyName);

        final Map<String, ClassNode> inverseHasManyMap = lookupStringToClassNodeMap(
                associatedClass, GrailsDomainClassProperty.HAS_MANY);
        final Map<String, ClassNode> inverseHasOneMap = lookupStringToClassNodeMap(
                associatedClass, GrailsDomainClassProperty.HAS_ONE);

        if (inverseHasManyMap.containsValue(classNode)) {
            for (String inversePropertyName : inverseHasManyMap.keySet()) {
                if (classNode.equals(inverseHasManyMap.get(inversePropertyName))) {
                    final AnnotationNode manyToOneAnnotation = new AnnotationNode(
                            new ClassNode(ManyToOne.class));
                    manyToOneAnnotation.addMember("cascade", EXPR_CASCADE_PERSIST);
                    annotateProperty(classNode, propertyName, manyToOneAnnotation);
                }
            }
        }
        else if (inverseHasOneMap.containsValue(classNode)) {
            for (String inversePropertyName : inverseHasOneMap.keySet()) {
                if (classNode.equals(inverseHasOneMap.get(inversePropertyName))) {
                    final AnnotationNode oneToOneAnnotation = new AnnotationNode(
                            new ClassNode(OneToOne.class));
                    oneToOneAnnotation.addMember("mappedBy", new ConstantExpression(inversePropertyName));
                    annotateProperty(classNode, propertyName, oneToOneAnnotation);
                }
            }
        }
        else {
            AnnotationNode annotationNode = new AnnotationNode(new ClassNode(ManyToOne.class));
            annotationNode.addMember("cascade", EXPR_CASCADE_ALL);
            annotateProperty(classNode, propertyName, annotationNode);
        }
    }

    private static List<ClassNode> getBelongsToList(ClassNode classNode) {
        PropertyNode propertyNode = classNode.getProperty(GrailsDomainClassProperty.BELONGS_TO);

        List<ClassNode> classNodes = new ArrayList<ClassNode>();
        if (propertyNode != null && propertyNode.isStatic()) {
            final Expression initialExpression = propertyNode.getInitialExpression();
            if (initialExpression instanceof ListExpression) {
                for (Expression expr : ((ListExpression) initialExpression).getExpressions()) {
                    if (expr instanceof ClassExpression) {
                        classNodes.add(expr.getType());
                    }
                }
            }
            else if (initialExpression instanceof ClassExpression) {
                classNodes.add(initialExpression.getType());
            }
        }
        return classNodes;
    }

    private static Map<String, String> lookupStringToStringMap(
            ClassNode classNode, String mapName) {

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

    private static Map<String, ClassNode> lookupStringToClassNodeMap(
            ClassNode classNode, String mapName) {

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
        AnnotationNode ann = new AnnotationNode(new ClassNode(annotationClass));

        final List<AnnotationNode> annotations = classNode.getAnnotations();
        if (annotations != null) {
            for (AnnotationNode annotationNode : annotations) {
                if (annotationNode.equals(ann)) return;
            }
        }

        classNode.addAnnotation(ann);
    }

    protected static void annotateAllProperties(ClassNode classNode,
            List<String> propertyNames, final Class<?> annotation) {
        final AnnotationNode annotationNode = new AnnotationNode(new ClassNode(annotation));
        annotateAllProperties(classNode, propertyNames, annotationNode);
    }

    protected static void annotateAllProperties(ClassNode classNode,
            Collection<String> propertyNames, final AnnotationNode annotationNode) {
        for (String propertyName : propertyNames) {
            annotateProperty(classNode, propertyName, annotationNode);
        }
    }

    protected static void annotateProperty(ClassNode classNode, String propertyName, Class<?> annotation) {
        annotateProperty(classNode,propertyName,new AnnotationNode(new ClassNode(annotation)));
    }

    protected static void annotateProperty(ClassNode classNode,
            String propertyName, final AnnotationNode annotationNode) {
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

    protected static void populateConstantList(List<String> theList,
            final PropertyNode theProperty) {
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
        }
        else if (initialExpression instanceof ConstantExpression) {
            addConstantExpressionToList(theList, initialExpression);
        }
    }

    protected static void addConstantExpressionToList(List<String> theList,
            Expression expression) {
        final Object val = ((ConstantExpression) expression).getValue();
        if (val != null) {
            theList.add(val.toString());
        }
    }
}
