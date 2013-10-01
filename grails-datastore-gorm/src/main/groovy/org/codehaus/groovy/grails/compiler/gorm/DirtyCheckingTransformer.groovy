package org.codehaus.groovy.grails.compiler.gorm

import grails.artefact.Artefact
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.GenericsType
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.NotExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.tools.GenericsUtils
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil
import org.codehaus.groovy.grails.compiler.injection.AstTransformer
import org.codehaus.groovy.grails.compiler.injection.GrailsArtefactClassInjector

import static org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils.*
import org.codehaus.groovy.grails.compiler.injection.GrailsDomainClassInjector
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.reflect.NameUtils

import static java.lang.reflect.Modifier.*
import org.codehaus.groovy.ast.AnnotationNode
import grails.persistence.PersistenceMethod

/**
 *
 * Transforms a domain class making it possible for the domain class to take responsibility of tracking changes to itself, thus removing the responsibility from the ORM system which would have to maintain parallel state
 * and compare the state of the domain class to the stored state. With this transformation the storage of the state is not necessary as the state is kept in the domain class itself
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@AstTransformer
@CompileStatic
class DirtyCheckingTransformer implements GrailsDomainClassInjector, GrailsArtefactClassInjector {
    private static final String VOID = "void";
    public static final String CHANGE_TRACKING_FIELD_NAME = '$changedProperties'
    private static final Class<?>[] EMPTY_JAVA_CLASS_ARRAY = [];
    private static final Class<?>[] OBJECT_CLASS_ARG = [Object.class];
    public static final String METHOD_NAME_TRACK_CHANGES = "trackChanges"
    public static final String METHOD_NAME_MARK_DIRTY = "markDirty"
    public static final String METHOD_NAME_RESET_DIRTY = "resetDirty"
    public static final String METHOD_NAME_IS_DIRTY = "hasChanged"
    public static final ConstantExpression CONSTANT_NULL = new ConstantExpression(null)
    public static final String METHOD_NAME_GET_DIRTY_PROPERTY_NAMES = "listDirtyPropertyNames"
    public static final String METHOD_NAME_GET_PERSISTENT_VALUE = "getOriginalValue"

    @Override
    void performInjectionOnAnnotatedClass(SourceUnit source, ClassNode classNode) {
        // First add a local field that will store the change tracking state. The field is a simple list of property names that have changed
        // the field is only added to root clauses that extend from java.lang.Object
        final changeTrackingVariable = new VariableExpression(CHANGE_TRACKING_FIELD_NAME)
        final mapClassNode = new ClassNode(Map).getPlainNodeReference()
        final changeTrackableClassNode = new ClassNode(DirtyCheckable).getPlainNodeReference()

        MethodNode putInMapMethodNode = mapClassNode.getMethods("put")[0]
        MethodNode containsKeyMethodNode = mapClassNode.getMethods("containsKey")[0]
        final markDirtyMethodNode = changeTrackableClassNode.getMethod(METHOD_NAME_MARK_DIRTY, new Parameter(ClassHelper.STRING_TYPE, "propertyName"))

        final shouldWeave = classNode.getSuperClass().equals(OBJECT_CLASS_NODE)
        if (shouldWeave) {
            FieldNode changingTrackingField = new FieldNode(CHANGE_TRACKING_FIELD_NAME, (PROTECTED | TRANSIENT).intValue(), mapClassNode, classNode, null);
            if(!classNode.getField(CHANGE_TRACKING_FIELD_NAME)) {
                classNode.addField(changingTrackingField)
            }

            final persistenceMethodAnnotation = new AnnotationNode(new ClassNode(PersistenceMethod.class).getPlainNodeReference())
            // we also need to make it implement the ChangeTrackable interface

            // Implement the trackChanges method such that:
            // void trackChanges() { if( !$changedProperties ) $changedProperties = [:] }
            classNode.addInterface(changeTrackableClassNode)
            final trackChangesBody = new BlockStatement()
            final assignChangeTrackingFieldStatement = new ExpressionStatement(
                    new BinaryExpression(changeTrackingVariable,
                            Token.newSymbol(Types.EQUAL, 0, 0),
                            new MapExpression())
            )

            trackChangesBody.addStatement(assignChangeTrackingFieldStatement)
            if(!classNode.getMethod(METHOD_NAME_TRACK_CHANGES, ZERO_PARAMETERS)) {

                final method = classNode.addMethod(METHOD_NAME_TRACK_CHANGES, PUBLIC, ClassHelper.VOID_TYPE, ZERO_PARAMETERS, null, trackChangesBody)
                method.addAnnotation(persistenceMethodAnnotation)
            }

            // Implement the hasChanged method such that:
            // boolean hasChanged() { $changedProperties == null || $changedProperties }
            if(!classNode.getMethod(METHOD_NAME_IS_DIRTY, ZERO_PARAMETERS)) {
                final leftExpression = new BinaryExpression(changeTrackingVariable, Token.newSymbol(Types.COMPARE_EQUAL, 0, 0), CONSTANT_NULL)
                final rightExpression = changeTrackingVariable
                final fullCondition = new BinaryExpression(leftExpression, Token.newSymbol(Types.LOGICAL_OR, 0, 0), rightExpression)
                classNode.addMethod(METHOD_NAME_IS_DIRTY, PUBLIC, ClassHelper.boolean_TYPE, ZERO_PARAMETERS, null, new ReturnStatement(fullCondition))
            }

            // Implement the hasChanged(String propertyName) method such that
            // boolean hasChanged() { $changedProperties == null ||  $changedProperties && $changedProperties.containsKey(propertyName) }
            final propertyNameParameter = new Parameter(ClassHelper.STRING_TYPE, "propertyName")
            final propertyNameParameterArray = [propertyNameParameter] as Parameter[]
            if(!classNode.getMethod(METHOD_NAME_IS_DIRTY, propertyNameParameterArray )) {
                final containsMethodCallExpression = new MethodCallExpression(changeTrackingVariable, "contains", new VariableExpression(propertyNameParameter))
                containsMethodCallExpression.methodTarget = containsKeyMethodNode
                final leftExpression = new BinaryExpression(changeTrackingVariable, Token.newSymbol(Types.COMPARE_EQUAL, 0, 0), CONSTANT_NULL)
                final rightExpression = new BinaryExpression(changeTrackingVariable, Token.newSymbol(Types.LOGICAL_AND, 0, 0), containsMethodCallExpression)
                final fullCondition = new BinaryExpression(leftExpression, Token.newSymbol(Types.LOGICAL_OR, 0, 0), rightExpression)
                final newMethod = classNode.addMethod(METHOD_NAME_IS_DIRTY, PUBLIC, ClassHelper.boolean_TYPE, propertyNameParameterArray, null, new ReturnStatement(fullCondition))
                final scope = new VariableScope()
                scope.putReferencedClassVariable(propertyNameParameter)
                newMethod.setVariableScope(scope)
            }

            // Implement the markDiry() method such that
            // void markDirty() { if( $changeProperties != null) $changeProperties.put className, '$DIRTY_MARKER' }
            final markDirtyBody = new BlockStatement()

            final addMethodCall = new MethodCallExpression(changeTrackingVariable, putInMapMethodNode.name, new ArgumentListExpression(new ConstantExpression(classNode.name), new ConstantExpression('$DIRTY_MARKER')))
            addMethodCall.setMethodTarget(putInMapMethodNode)
            final changeTrackingVariableNullCheck = new BooleanExpression(new BinaryExpression(changeTrackingVariable, Token.newSymbol(Types.COMPARE_NOT_EQUAL, 0, 0), CONSTANT_NULL))
            def ifNotNullStatement = new IfStatement(changeTrackingVariableNullCheck, new ExpressionStatement(addMethodCall), new EmptyStatement())
            markDirtyBody.addStatement(ifNotNullStatement)
            if(!classNode.getMethod(METHOD_NAME_MARK_DIRTY, ZERO_PARAMETERS)) {

                final method = classNode.addMethod(METHOD_NAME_MARK_DIRTY, PUBLIC, ClassHelper.VOID_TYPE, ZERO_PARAMETERS, null, markDirtyBody)
                method.addAnnotation(persistenceMethodAnnotation)
            }

            // Implement the markDirty(String propertyName) method such that
            // void markDirty(String propertyName) { if( $changeProperties != null)  $changeProperties.put propertyName, getProperty(propertyName) }
            if(!classNode.getMethod(METHOD_NAME_MARK_DIRTY, propertyNameParameterArray)) {
                final markPropertyDirtyBody = new BlockStatement()
                final propertyNameVar = new VariableExpression(propertyNameParameter)
                final addPropertyMethodCall = new MethodCallExpression(changeTrackingVariable, putInMapMethodNode.name, new ArgumentListExpression(propertyNameVar, new MethodCallExpression(THIS_EXPR, "getProperty", propertyNameVar)))
                addPropertyMethodCall.methodTarget = putInMapMethodNode
                final containsKeyMethodCall = new MethodCallExpression(changeTrackingVariable, containsKeyMethodNode.name, propertyNameVar)
                containsKeyMethodCall.methodTarget = containsKeyMethodNode
                ifNotNullStatement = new IfStatement(new BooleanExpression(new BinaryExpression(changeTrackingVariableNullCheck, Token.newSymbol(Types.LOGICAL_AND, 0, 0), new NotExpression(containsKeyMethodCall))), new ExpressionStatement(addPropertyMethodCall), new EmptyStatement())
                markPropertyDirtyBody.addStatement(ifNotNullStatement)
                final newMethod = classNode.addMethod(METHOD_NAME_MARK_DIRTY, PUBLIC, ClassHelper.VOID_TYPE, propertyNameParameterArray, null, markPropertyDirtyBody)
                final scope = new VariableScope()
                scope.putReferencedClassVariable(propertyNameParameter)
                newMethod.setVariableScope(scope)
                newMethod.addAnnotation(persistenceMethodAnnotation)

            }

            // Implement listDirtyProperties() such that
            // List<String> listDirtyProperties() { trackChanges(); $changeProperties.keySet().toList() }
            if(!classNode.getMethod(METHOD_NAME_GET_DIRTY_PROPERTY_NAMES, ZERO_PARAMETERS)) {
                final methodBody = new BlockStatement()
                final returnDirtyPropertyNames = new ReturnStatement(new MethodCallExpression(new MethodCallExpression(changeTrackingVariable, "keySet", ZERO_ARGS), "toList", ZERO_ARGS))
                methodBody.addStatement(new IfStatement(new BooleanExpression(changeTrackingVariable), returnDirtyPropertyNames, new ReturnStatement(new ListExpression())))
                final newMethod = classNode.addMethod(METHOD_NAME_GET_DIRTY_PROPERTY_NAMES, PUBLIC, ClassHelper.make(List).getPlainNodeReference(), ZERO_PARAMETERS, null, methodBody)
                newMethod.addAnnotation(persistenceMethodAnnotation)
            }

            // Implement getOriginalValue(String) such that
            // Object getOriginalValue(String propertyName) { $changedProperties.get(propertyName) }
            if(!classNode.getMethod(METHOD_NAME_GET_PERSISTENT_VALUE, propertyNameParameterArray)) {

                final methodBody = new BlockStatement()
                final propertyNameVariable = new VariableExpression(propertyNameParameter)
                final containsKeyMethodCall = new MethodCallExpression(changeTrackingVariable, containsKeyMethodNode.name, propertyNameVariable)
                containsKeyMethodCall.methodTarget = containsKeyMethodNode
                methodBody.addStatement(new IfStatement(new BooleanExpression(containsKeyMethodCall),
                        new ExpressionStatement( new MethodCallExpression(changeTrackingVariable, "get", propertyNameVariable) ),
                        new ReturnStatement(new MethodCallExpression(THIS_EXPR, "getProperty", propertyNameVariable))))
                final newMethod = classNode.addMethod(METHOD_NAME_GET_PERSISTENT_VALUE, PUBLIC, ClassHelper.OBJECT_TYPE, propertyNameParameterArray, null, methodBody)
                newMethod.addAnnotation(persistenceMethodAnnotation)
            }
            // Now we go through all the properties, if the property is a persistent property and change tracking has been initiated then we add to the setter of the property
            // code that will mark the property as dirty. Note that if the property has no getter we have to add one, since only adding the setter results in a read-only property
            final propertyNodes = classNode.getProperties()

            Map<String, GetterAndSetter> gettersAndSetters = [:]

            for (MethodNode mn in classNode.methods) {
                final methodName = mn.name
                if(!mn.isPublic() || mn.isStatic() || mn.isSynthetic()) continue

                if (isSetter(methodName, mn)) {
                    String propertyName = NameUtils.getPropertyNameForGetterOrSetter(methodName)
                    GetterAndSetter getterAndSetter = getGetterAndSetterForPropertyName(gettersAndSetters, propertyName)
                    getterAndSetter.setter = mn
                } else if (isGetter(methodName, mn)) {
                    String propertyName = NameUtils.getPropertyNameForGetterOrSetter(methodName)
                    GetterAndSetter getterAndSetter = getGetterAndSetterForPropertyName(gettersAndSetters, propertyName)
                    getterAndSetter.getter = mn
                }
            }

            for (PropertyNode pn in propertyNodes) {
                final propertyName = pn.name
                if (!pn.isStatic() && pn.isPublic() && !GrailsDomainConfigurationUtil.isConfigurational(propertyName)) {
                    if(isTransient(pn.modifiers) || isFinal(pn.modifiers)) continue

                    final getterAndSetter = gettersAndSetters[propertyName]

                    // if there is no explicit getter and setter then one will be generated by Groovy, so we must add these to track changes
                    if(getterAndSetter == null) {
                        final propertyField = pn.getField()

                        // first add the getter
                        ClassNode originalReturnType = pn.getType()
                        ClassNode returnType;
                        if(!originalReturnType.getNameWithoutPackage().equals(VOID)) {
                            if(ClassHelper.isPrimitiveType(originalReturnType.redirect())) {
                                returnType = originalReturnType.getPlainNodeReference()
                            } else {
                                returnType = alignReturnType(classNode, originalReturnType);
                            }
                        }
                        else {
                            returnType = originalReturnType
                        }
                        final getterName = NameUtils.getGetterName(propertyName, boolean.class.getName().equals(returnType.getName()))
                        classNode.addMethod(getterName, PUBLIC, returnType, ZERO_PARAMETERS, null, new ReturnStatement(new VariableExpression(propertyField.getName())))

                        // now add the setter that tracks changes. Each setters becomes:
                        // void setFoo(String foo) { markDirty("foo"); this.foo = foo }
                        final setterName = NameUtils.getSetterName(propertyName)
                        final setterParameter = new Parameter(returnType, propertyName)
                        final setterBody = new BlockStatement()
                        MethodCallExpression markDirtyMethodCall = createMarkDirtyMethodCall(markDirtyMethodNode, propertyName)
                        setterBody.addStatement(new ExpressionStatement(markDirtyMethodCall))
                        setterBody.addStatement(  new ExpressionStatement(
                                new BinaryExpression(new PropertyExpression(THIS_EXPR, propertyField.name),
                                        Token.newSymbol(Types.EQUAL, 0, 0),
                                        new VariableExpression(setterParameter))
                        )
                        )

                        classNode.addMethod(setterName, PUBLIC, ClassHelper.VOID_TYPE, [setterParameter] as Parameter[], null, setterBody)
                    }
                    else if(getterAndSetter.hasBoth()) {
                        // if both a setter and getter are present, we get hold of the setter and weave the markDirty method call into it
                        weaveIntoExistingSetter(propertyName, getterAndSetter, markDirtyMethodNode)
                    }
                    else {
                        // there isn't both a getter and a setter then this is not a candidate for persistence, so we eliminate it from change tracking
                        gettersAndSetters.remove(propertyName)
                    }
                }
            }

            // We also need to search properties that are represented as getters with setters. This requires going through all the methods and finding getter/setter pairs that are public
            gettersAndSetters.each { String propertyName, GetterAndSetter getterAndSetter ->
                if(!GrailsDomainConfigurationUtil.isConfigurational(propertyName) && getterAndSetter.hasBoth()) {
                    weaveIntoExistingSetter(propertyName, getterAndSetter, markDirtyMethodNode)
                }
            }
        }
    }

    @Override
    void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        if (!classNode.getAnnotations(new ClassNode(Artefact.class)).isEmpty()) return;

        performInjectionOnAnnotatedClass(source, classNode)
    }

    @Override
    void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode)
    }

    public boolean shouldInject(URL url) {
        return GrailsResourceUtils.isDomainClass(url);
    }

    @Override
    void performInjectionOnAnnotatedEntity(ClassNode classNode) {
        performInjectionOnAnnotatedClass(null, classNode)
    }


    private static ClassNode alignReturnType(final ClassNode receiver, final ClassNode originalReturnType) {
        ClassNode copiedReturnType = originalReturnType.getPlainNodeReference();

        final genericTypes = originalReturnType.getGenericsTypes()
        if (genericTypes) {
            List<GenericsType> newGenericTypes = []

            for(GenericsType gt in genericTypes) {
                ClassNode[] upperBounds = null
                if (gt.upperBounds) {
                    upperBounds = gt.upperBounds.collect { ClassNode cn -> cn.plainNodeReference } as ClassNode[]
                }
                newGenericTypes << new GenericsType(gt.type.plainNodeReference, upperBounds, gt.lowerBound?.plainNodeReference)
            }
            copiedReturnType.setGenericsTypes(newGenericTypes as GenericsType[])
        }

        return copiedReturnType;
    }
    protected void weaveIntoExistingSetter(String propertyName, GetterAndSetter getterAndSetter, MethodNode markDirtyMethodNode) {
        final setterMethod = getterAndSetter.setter
        if(setterMethod.getAnnotations(new ClassNode(PersistenceMethod))) return

        final currentBody = setterMethod.code
        final setterParameter = setterMethod.getParameters()[0]
        MethodCallExpression markDirtyMethodCall = createMarkDirtyMethodCall(markDirtyMethodNode, propertyName)
        final newBody = new BlockStatement()
        newBody.addStatement(new ExpressionStatement(markDirtyMethodCall))
        newBody.addStatement(currentBody)
        setterMethod.code = newBody
    }

    protected MethodCallExpression createMarkDirtyMethodCall(MethodNode markDirtyMethodNode, String propertyName) {
        final markDirtyMethodCall = new MethodCallExpression(THIS_EXPR, markDirtyMethodNode.name, new ConstantExpression(propertyName))
        markDirtyMethodCall.methodTarget = markDirtyMethodNode
        markDirtyMethodCall
    }

    protected GetterAndSetter getGetterAndSetterForPropertyName(LinkedHashMap<String, GetterAndSetter> gettersAndSetters, String propertyName) {
        def getterAndSetter = gettersAndSetters[propertyName]
        if (getterAndSetter == null) {
            getterAndSetter = new GetterAndSetter()
            gettersAndSetters[propertyName] = getterAndSetter
        }
        getterAndSetter
    }



    private boolean isSetter(String methodName, MethodNode declaredMethod) {
        return declaredMethod.getParameters().length == 1 && GrailsClassUtils.isSetter(methodName, OBJECT_CLASS_ARG);
    }

    private boolean isGetter(String methodName, MethodNode declaredMethod) {
        return declaredMethod.getParameters().length == 0 && GrailsClassUtils.isGetter(methodName, EMPTY_JAVA_CLASS_ARRAY);
    }

    @Override
    String[] getArtefactTypes() {
        return [DomainClassArtefactHandler.TYPE] as String[];
    }

    @CompileStatic
    class GetterAndSetter {
        MethodNode getter
        MethodNode setter

        boolean hasBoth() {
            getter && setter
        }

        boolean hasNeither() {
            !getter && !setter
        }
    }
}
