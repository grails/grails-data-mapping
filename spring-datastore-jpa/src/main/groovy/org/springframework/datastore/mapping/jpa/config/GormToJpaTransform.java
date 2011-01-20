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

package org.springframework.datastore.mapping.jpa.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.springframework.datastore.mapping.model.MappingFactory;

/**
 * A AST transformation that turns a GORM entity into a JPA entity
 * 
 * @author Graeme Rocher
 * @since 1.0
 *
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class GormToJpaTransform implements ASTTransformation{

	private static final AnnotationNode ANNOTATION_TEMPORAL = new AnnotationNode(new ClassNode(Temporal.class));
	private static final AnnotationNode ANNOTATION_MANY_TO_ONE = new AnnotationNode(new ClassNode(ManyToOne.class));
	private static final AnnotationNode ANNOTATION_VERSION = new AnnotationNode(new ClassNode(Version.class));
	private static final AnnotationNode ANNOTATION_ID = new AnnotationNode(new ClassNode(Id.class));
	private static final AnnotationNode ANNOTATION_ENTITY = new AnnotationNode(new ClassNode(Entity.class));
	private static final AnnotationNode ANNOTATION_BASIC = new AnnotationNode(new ClassNode(Basic.class));
	
	
	private static final PropertyExpression EXPR_CASCADE_ALL = new PropertyExpression(new ClassExpression(new ClassNode(CascadeType.class)), "ALL");
    private static final PropertyExpression EXPR_CASCADE_PERSIST = new PropertyExpression(new ClassExpression(new ClassNode(CascadeType.class)), "PERSIST");
    
	private static final ClassNode MY_TYPE = new ClassNode(JpaEntity.class);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();

	@Override
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

        transformEntity(source, cNode);
	}

	public static void transformEntity(SourceUnit source, ClassNode classNode) {
		
		// add the JPA @Entity annotation
		classNode.addAnnotation(ANNOTATION_ENTITY);
		
		// annotate the id property with @Id
		PropertyNode idProperty = classNode.getProperty(GrailsDomainClassProperty.IDENTITY);
		
		final FieldNode idField = idProperty.getField();
		if(idProperty != null) {
			
			idField.addAnnotation(ANNOTATION_ID);
			final AnnotationNode generatedValueAnnotation = new AnnotationNode(new ClassNode(GeneratedValue.class));
			
			idField.addAnnotation(generatedValueAnnotation);
		}
		
		// annotate the version property with @Version
		PropertyNode versionProperty = classNode.getProperty(GrailsDomainClassProperty.VERSION);
		if(versionProperty != null) {
			idField.addAnnotation(ANNOTATION_VERSION);
		}
		
		final List<PropertyNode> properties = classNode.getProperties();
		for (PropertyNode propertyNode : properties) {		
			if(propertyNode.isPublic() && !propertyNode.isStatic()) {
				if(propertyNode != idProperty && propertyNode != versionProperty) {					
					final String typeName = propertyNode.getType().getName();
					
					if(typeName.equals("java.util.Date") || typeName.equals("java.util.Calendar")) {
						propertyNode.getField().addAnnotation(ANNOTATION_TEMPORAL);
					}
					else if(MappingFactory.isSimpleType(typeName)) {
						propertyNode.getField().addAnnotation(ANNOTATION_BASIC);
					}
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
		
		if(embeddedProp != null) {
			for (String propertyName : propertyNameList) {
				final PropertyNode property = classNode.getProperty(propertyName);
				if(property != null) {
					
					ClassNode embeddedType = property.getField().getType();
					annotateIfNecessary(embeddedType, Embeddable.class);
				}
			}

		}
		
		Map<String, ClassNode> hasManyMap = lookupStringToClassNodeMap(classNode, GrailsDomainClassProperty.HAS_MANY);
		Map<String, ClassNode> hasOneMap = lookupStringToClassNodeMap(classNode, GrailsDomainClassProperty.HAS_ONE);
		Map<String, ClassNode> belongsToMap = lookupStringToClassNodeMap(classNode, GrailsDomainClassProperty.BELONGS_TO);
		Map<String, String> mappedByMap = lookupStringToStringMap(classNode, GrailsDomainClassProperty.MAPPED_BY);
		
		if(!belongsToMap.isEmpty()) {
			for (String propertyName : belongsToMap.keySet()) {
				ClassNode associatedClass = belongsToMap.get(propertyName);
				
				final Map<String, ClassNode> inverseHasManyMap = lookupStringToClassNodeMap(associatedClass, GrailsDomainClassProperty.HAS_MANY);
				final Map<String, ClassNode> inverseHasOneMap = lookupStringToClassNodeMap(associatedClass, GrailsDomainClassProperty.HAS_ONE);
				
				if(inverseHasManyMap.containsValue(classNode)) {
					for (String inversePropertyName : inverseHasManyMap.keySet()) {
						if(classNode.equals(inverseHasManyMap.get(inversePropertyName))) {
							annotateProperty(classNode, propertyName, ANNOTATION_MANY_TO_ONE);
						}
					}
				}
				else if(inverseHasOneMap.containsValue(classNode)) {
					for (String inversePropertyName : inverseHasOneMap.keySet()) {
						if(classNode.equals(inverseHasOneMap.get(inversePropertyName))) {
							final AnnotationNode oneToOneAnnotation = new AnnotationNode(new ClassNode(OneToOne.class));
							oneToOneAnnotation.addMember("mappedBy", new ConstantExpression(inversePropertyName));
							annotateProperty(classNode, propertyName, oneToOneAnnotation);
						}
					}					
				}
				
			}
		}
		if(!hasOneMap.isEmpty()) {
			for (String propertyName : hasOneMap.keySet()) {
				final AnnotationNode oneToOneAnnotation = new AnnotationNode(new ClassNode(OneToOne.class));
				oneToOneAnnotation.addMember("optional", ConstantExpression.FALSE);
				oneToOneAnnotation.addMember("cascade", EXPR_CASCADE_ALL);
				annotateProperty(classNode, propertyName, oneToOneAnnotation);
			}
		}
		if(!hasManyMap.isEmpty()) {
			for (String propertyName : hasManyMap.keySet()) {
				ClassNode associatedClass = hasManyMap.get(propertyName);
				final Map<String, ClassNode> inverseBelongsToMap = lookupStringToClassNodeMap(associatedClass, GrailsDomainClassProperty.BELONGS_TO);
				final Map<String, ClassNode> inverseHasManyMap = lookupStringToClassNodeMap(associatedClass, GrailsDomainClassProperty.HAS_MANY);
				
				
				
				final AnnotationNode oneToManyAnnotation = new AnnotationNode(new ClassNode(OneToMany.class));			
				oneToManyAnnotation.addMember("targetEntity", new ClassExpression(associatedClass));
				
				if(mappedByMap.containsKey(propertyName)) {
					oneToManyAnnotation.addMember("mappedBy", new ConstantExpression(mappedByMap.get(propertyName)));
					oneToManyAnnotation.addMember("cascade", EXPR_CASCADE_PERSIST);
					annotateProperty(classNode, propertyName, oneToManyAnnotation);
				}
				else {
					if(inverseHasManyMap.containsValue(classNode)) {
						// many-to-many association
						List<ClassNode> belongsToList = getBelongsToList(classNode);
						
						final AnnotationNode manyToManyAnnotation = new AnnotationNode(new ClassNode(ManyToMany.class));
						manyToManyAnnotation.addMember("targetEntity", new ClassExpression(associatedClass));
						if(belongsToList.contains(associatedClass)) {
							for (String inversePropertyName : inverseHasManyMap.keySet()) {
								if(classNode.equals(inverseHasManyMap.get(inversePropertyName))) {
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
					else if(inverseBelongsToMap.containsValue(classNode)) {
						for (String inversePropertyName : inverseBelongsToMap.keySet()) {
							if(classNode.equals(inverseBelongsToMap.get(inversePropertyName))) {
								oneToManyAnnotation.addMember("mappedBy", new ConstantExpression(inversePropertyName));
								oneToManyAnnotation.addMember("cascade", EXPR_CASCADE_ALL);
							}
						}
						annotateProperty(classNode, propertyName, oneToManyAnnotation);
					}
					else {
						// unidrectional one-to-many
						oneToManyAnnotation.addMember("cascade", EXPR_CASCADE_ALL);
						annotateProperty(classNode, propertyName, oneToManyAnnotation);
					}
				}
				
				
			}

		}
	}

	private static List<ClassNode> getBelongsToList(ClassNode classNode) {
		PropertyNode propertyNode = classNode.getProperty(GrailsDomainClassProperty.BELONGS_TO);
		
		List<ClassNode> classNodes = new ArrayList<ClassNode>();
		if(propertyNode != null && propertyNode.isStatic()) {
			final Expression initialExpression = propertyNode.getInitialExpression();
			if(initialExpression instanceof ListExpression) {
				for (Expression expr : ((ListExpression) initialExpression).getExpressions()) {
					if(expr instanceof ClassExpression) {
						classNodes.add(expr.getType());
					}
				}
			}
			else if(initialExpression instanceof ClassExpression) {
				classNodes.add(initialExpression.getType());
			}
		}
		return classNodes;
	}

	private static Map<String, String> lookupStringToStringMap(
			ClassNode classNode, String mapName) {
		Map<String, String> stringToClassNodeMap = new HashMap<String, String>();
		final PropertyNode mapProperty = classNode.getProperty(mapName);
		if(mapProperty != null) {
			final Expression initialExpression = mapProperty.getInitialExpression();
			if(initialExpression instanceof MapExpression) {
				MapExpression mapExpr = (MapExpression) initialExpression;
				final List<MapEntryExpression> mapEntryExpressions = mapExpr.getMapEntryExpressions();
				for (MapEntryExpression mapEntryExpression : mapEntryExpressions) {
					final Expression keyExpression = mapEntryExpression.getKeyExpression();
					if(keyExpression instanceof ConstantExpression) {
						ConstantExpression ce = (ConstantExpression) keyExpression;
						String propertyName = ce.getValue().toString();
						final Expression valueExpression = mapEntryExpression.getValueExpression();
						if(valueExpression instanceof ConstantExpression) {
							stringToClassNodeMap.put(propertyName, ((ConstantExpression) valueExpression).getValue().toString());
						}
						 
					}
				}
			}
		}
		return stringToClassNodeMap;
	}

	private static Map<String, ClassNode> lookupStringToClassNodeMap(
			ClassNode classNode, String mapName) {
		Map<String, ClassNode> stringToClassNodeMap = new HashMap<String, ClassNode>();
		final PropertyNode mapProperty = classNode.getProperty(mapName);
		if(mapProperty != null) {
			final Expression initialExpression = mapProperty.getInitialExpression();
			if(initialExpression instanceof MapExpression) {
				MapExpression mapExpr = (MapExpression) initialExpression;
				final List<MapEntryExpression> mapEntryExpressions = mapExpr.getMapEntryExpressions();
				for (MapEntryExpression mapEntryExpression : mapEntryExpressions) {
					final Expression keyExpression = mapEntryExpression.getKeyExpression();
					if(keyExpression instanceof ConstantExpression) {
						ConstantExpression ce = (ConstantExpression) keyExpression;
						String propertyName = ce.getValue().toString();
						final Expression valueExpression = mapEntryExpression.getValueExpression();
						if(valueExpression instanceof ClassExpression) {
							ClassExpression clsExpr = (ClassExpression) valueExpression;
							stringToClassNodeMap.put(propertyName, clsExpr.getType());
						}
						 
					}
				}
			}
		}
		return stringToClassNodeMap;
	}



	private static void annotateIfNecessary(ClassNode classNode,
			Class<?> annotationClass) {
		AnnotationNode ann = new AnnotationNode(new ClassNode(annotationClass));
		
		final List<AnnotationNode> annotations = classNode.getAnnotations();
		if(annotations != null) {
			for (AnnotationNode annotationNode : annotations) {
				if(annotationNode.equals(ann)) return;
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

	protected static void annotateProperty(ClassNode classNode,
			String propertyName, final AnnotationNode annotationNode) {
		final PropertyNode prop = classNode.getProperty(propertyName);
		if(prop != null) {
			prop.getField().addAnnotation(annotationNode);
		}
	}

	protected static void populateConstantList(List<String> theList,
			final PropertyNode theProperty) {
		if(theProperty != null ) {
			final Expression initialExpression = theProperty.getInitialExpression();
			if(initialExpression instanceof ListExpression) {
				ListExpression listExpression = (ListExpression) initialExpression;
				final List<Expression> entries = listExpression.getExpressions();
				for (Expression expression : entries) {
					if(expression != null) {
						if(expression instanceof ConstantExpression) {
							addConstantExpressionToList(theList, expression);
						}
					}
				}
			}
			else if(initialExpression instanceof ConstantExpression) {
				addConstantExpressionToList(theList, initialExpression);
			}
		}
	}

	protected static void addConstantExpressionToList(List<String> theList,
			Expression expression) {
		final Object val = ((ConstantExpression) expression).getValue();
		if(val != null) theList.add(val.toString());
	}

	
}
