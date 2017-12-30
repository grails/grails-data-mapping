package org.grails.datastore.gorm.validation.constraints.builder;


import grails.gorm.validation.ConstrainedProperty;
import grails.gorm.validation.Constraint;
import groovy.lang.*;
import groovy.util.BuilderSupport;
import org.grails.datastore.gorm.validation.constraints.registry.ConstraintRegistry;
import grails.gorm.validation.DefaultConstrainedProperty;
import org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.InvalidPropertyException;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Builder used as a delegate within the "constraints" closure of GrailsDomainClass instances .
 *
 * @author Graeme Rocher
 */
public class ConstrainedPropertyBuilder extends BuilderSupport {

    private static final String SHARED_CONSTRAINT = "shared";
    private static final String IMPORT_FROM_CONSTRAINT = "importFrom";
    private static final Logger LOG = LoggerFactory.getLogger(ConstrainedPropertyBuilder.class);
    private final Map<String, ConstrainedProperty> constrainedProperties = new LinkedHashMap<>();
    private final Map<String, String> sharedConstraints = new HashMap<>();
    private int order = 1;
    private final Class<?> targetClass;
    private final ClassPropertyFetcher classPropertyFetcher;
    private final MetaClass targetMetaClass;
    private final ConstraintRegistry constraintRegistry;
    private final MappingContext mappingContext;
    private final Map<String, Object> defaultConstraints;
    private boolean allowDynamic = false;


    public ConstrainedPropertyBuilder(MappingContext mappingContext, ConstraintRegistry constraintRegistry, Class targetClass, Map<String, Object> defaultConstraints) {
        this.targetClass = targetClass;
        this.mappingContext = mappingContext;
        classPropertyFetcher = ClassPropertyFetcher.forClass(targetClass);
        targetMetaClass = GroovySystem.getMetaClassRegistry().getMetaClass(targetClass);
        this.constraintRegistry = constraintRegistry;
        this.defaultConstraints = defaultConstraints;
    }

    public String getSharedConstraint(String propertyName) {
        return sharedConstraints.get(propertyName);
    }

    @Override
    protected Object doInvokeMethod(String methodName, Object name, Object args) {
        try {
            return super.doInvokeMethod(methodName, name, args);
        } catch (MissingMethodException e) {
            return targetMetaClass.invokeMethod(targetClass, methodName, args);
        }
    }

    @Override
    public Object getProperty(String property) {
        try {
            return super.getProperty(property);
        } catch (MissingPropertyException e) {
            return targetMetaClass.getProperty(targetClass, property);
        }
    }

    @Override
    public void setProperty(String property, Object newValue) {
        try {
            super.setProperty(property, newValue);
        } catch (MissingPropertyException e) {
            targetMetaClass.setProperty(targetClass, property, newValue);
        }

    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Object createNode(Object name, Map attributes) {
        // we do this so that missing property exception is throw if it doesn't exist

        try {
            String property = (String)name;
            DefaultConstrainedProperty cp;
            if (constrainedProperties.containsKey(property)) {
                cp = (DefaultConstrainedProperty)constrainedProperties.get(property);
            }
            else {
                Class<?> propertyType = classPropertyFetcher.getPropertyType(property);
                if (propertyType == null) {
                    if(!allowDynamic) {
                        throw new MissingMethodException(property, targetClass, new Object[]{attributes}, true);
                    }
                    // assume in dynamic use types are strings
                    propertyType = CharSequence.class;
                }
                cp = new DefaultConstrainedProperty(targetClass, property, propertyType, constraintRegistry);
                cp.setOrder(order++);
                constrainedProperties.put(property, cp);
            }

            if (cp.getPropertyType() == null) {
                if (!IMPORT_FROM_CONSTRAINT.equals(name)) {
                    LOG.warn("Property [" + cp.getPropertyName() + "] not found in domain class " +
                            targetClass.getName() + "; cannot apply constraints: " + attributes);
                }
                return cp;
            }

            for (Object o : attributes.keySet()) {
                String constraintName = (String) o;
                final Object value = attributes.get(constraintName);
                if (SHARED_CONSTRAINT.equals(constraintName)) {
                    if (value != null) {
                        sharedConstraints.put(property, value.toString());
                    }
                    continue;
                }
                if (cp.supportsContraint(constraintName)) {
                    cp.applyConstraint(constraintName, value);
                }
                else {
                    if (!constraintRegistry.findConstraintFactories(constraintName).isEmpty()) {
                        // constraint is registered but doesn't support this property's type
                        LOG.warn("Property [" + cp.getPropertyName() + "] of domain class " +
                                targetClass.getName() + " has type [" + cp.getPropertyType().getName() +
                                "] and doesn't support constraint [" + constraintName +
                                "]. This constraint will not be checked during validation.");
                    }
                    else {
                        // in the case where the constraint is not supported we still retain meta data
                        // about the constraint in case its needed for other things
                        cp.addMetaConstraint(constraintName, value);
                    }
                }
            }

            return cp;
        }
        catch(InvalidPropertyException ipe) {
            throw new MissingMethodException((String)name,targetClass,new Object[]{ attributes});
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Object createNode(Object name, Map attributes, Object value) {
        if (IMPORT_FROM_CONSTRAINT.equals(name) && (value instanceof Class)) {
            return handleImportFrom(attributes, (Class) value);
        }
        throw new MissingMethodException((String)name,targetClass,new Object[]{ attributes,value});
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object handleImportFrom(Map attributes, Class importFromClazz) {

        Map importFromConstrainedProperties = new DefaultConstraintEvaluator(constraintRegistry,mappingContext, defaultConstraints )
                                                        .evaluate(importFromClazz);

        List<MetaProperty> metaProperties = classPropertyFetcher.getMetaProperties();

        List toBeIncludedPropertyNamesParam = (List) attributes.get("include");
        List toBeExcludedPropertyNamesParam = (List) attributes.get("exclude");

        List<String> resultingPropertyNames = new ArrayList<>();
        for (MetaProperty metaProperty : metaProperties) {
            if(Modifier.isStatic(metaProperty.getModifiers())) {
                continue;
            }
            String propertyName = metaProperty.getName();

            if (toBeIncludedPropertyNamesParam == null) {
                resultingPropertyNames.add(propertyName);
            }
            else if (isListOfRegexpsContainsString(toBeIncludedPropertyNamesParam, propertyName)) {
                resultingPropertyNames.add(propertyName);
            }

            if (toBeExcludedPropertyNamesParam != null
                    && isListOfRegexpsContainsString(toBeExcludedPropertyNamesParam, propertyName)) {
                resultingPropertyNames.remove(propertyName);
            }
        }

        resultingPropertyNames.remove(GormProperties.CLASS);
        resultingPropertyNames.remove(GormProperties.META_CLASS);

        for (String targetPropertyName : resultingPropertyNames) {
            DefaultConstrainedProperty importFromConstrainedProperty =
                    (DefaultConstrainedProperty) importFromConstrainedProperties.get(targetPropertyName);

            if (importFromConstrainedProperty != null) {
                Map importFromConstrainedPropertyAttributes = new HashMap();
                for (Constraint importFromAppliedConstraint : importFromConstrainedProperty.getAppliedConstraints()) {
                    String importFromAppliedConstraintName = importFromAppliedConstraint.getName();
                    Object importFromAppliedConstraintParameter = importFromAppliedConstraint.getParameter();
                    importFromConstrainedPropertyAttributes.put(
                            importFromAppliedConstraintName, importFromAppliedConstraintParameter);
                }
                //add any metaContraints
                importFromConstrainedPropertyAttributes.putAll(importFromConstrainedProperty.getMetaConstraints());
                
                createNode(targetPropertyName, importFromConstrainedPropertyAttributes);
            }
        }

        return null;
    }

    private boolean isListOfRegexpsContainsString(List<String> listOfStrings, String stringToMatch) {
        boolean result = false;

        for (String listElement:listOfStrings) {
            if (stringToMatch.matches(listElement)) {
                result = true;
                break;
            }
        }

        return result;
    }

    @Override
    protected void setParent(Object parent, Object child) {
        // do nothing
    }

    @Override
    protected Object createNode(Object name) {
        return createNode(name, Collections.EMPTY_MAP);
    }

    @Override
    protected Object createNode(Object name, Object value) {
        return createNode(name,Collections.EMPTY_MAP,value);
    }

    public Map<String, ConstrainedProperty> getConstrainedProperties() {
        return constrainedProperties;
    }

    public void setAllowDynamic(boolean allowDynamic) {
        this.allowDynamic = allowDynamic;
    }
}
