package org.codehaus.groovy.grails.compiler.gorm

import grails.artefact.Artefact
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.NotExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
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
    public static final String CHANGE_TRACKING_FIELD_NAME = '$changedProperties'
    private static final Class<?>[] EMPTY_JAVA_CLASS_ARRAY = [];
    private static final Class<?>[] OBJECT_CLASS_ARG = [Object.class];
    public static final String METHOD_NAME_TRACK_CHANGES = "trackChanges"
    public static final String METHOD_NAME_MARK_DIRTY = "markDirty"
    public static final String METHOD_NAME_HAS_CHANGED = "hasChanged"

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

    @Override
    void performInjectionOnAnnotatedClass(SourceUnit source, ClassNode classNode) {
        // First add a local field that will store the change tracking state. The field is a simple list of property names that have changed
        // the field is only added to root clauses that extend from java.lang.Object
        final changeTrackingVariable = new VariableExpression(CHANGE_TRACKING_FIELD_NAME)
        final setClassNode = new ClassNode(Set).getPlainNodeReference()
        final changeTrackableClassNode = new ClassNode(DirtyCheckable).getPlainNodeReference()

        MethodNode addToSetMethodNode = setClassNode.getMethods("add")[0]
        MethodNode containsSetMethodNode = setClassNode.getMethods("contains")[0]
        final markDirtyMethodNode = changeTrackableClassNode.getMethod(METHOD_NAME_MARK_DIRTY, new Parameter(ClassHelper.STRING_TYPE, "propertyName"))

        if (classNode.getSuperClass().equals(OBJECT_CLASS_NODE)) {
            FieldNode changingTrackingField = new FieldNode(CHANGE_TRACKING_FIELD_NAME, PROTECTED, setClassNode, classNode, null);
            classNode.addField(changingTrackingField)

            // we also need to make it implement the ChangeTrackable interface

            // Implement the startTracking method such that:
            // void startTracking() { $changedProperties = [] }
            classNode.addInterface(changeTrackableClassNode)
            final startTrackingBody = new BlockStatement()
            startTrackingBody.asBoolean()
            final assignChangeTrackingFieldStatement = new ExpressionStatement(
                    new BinaryExpression(changeTrackingVariable,
                            Token.newSymbol(Types.EQUAL, 0, 0),
                            new ListExpression())
            )

            startTrackingBody.addStatement(new IfStatement(new NotExpression(changeTrackingVariable), assignChangeTrackingFieldStatement,new EmptyStatement()))
            if(!classNode.getMethod(METHOD_NAME_TRACK_CHANGES, ZERO_PARAMETERS))
                classNode.addMethod(METHOD_NAME_TRACK_CHANGES, PUBLIC, ClassHelper.VOID_TYPE, ZERO_PARAMETERS, null, startTrackingBody)

            // Implement the hasChanged method such that:
            // boolean hasChanged() { $changedProperties }
            if(!classNode.getMethod(METHOD_NAME_HAS_CHANGED, ZERO_PARAMETERS))
                classNode.addMethod(METHOD_NAME_HAS_CHANGED, PUBLIC, ClassHelper.boolean_TYPE, ZERO_PARAMETERS, null, new ReturnStatement(changeTrackingVariable))

            // Implement the hasChanged(String propertyName) method such that
            // boolean hasChanged() { $changedProperties && $changedProperties.contains(propertyName) }
            final propertyNameParameter = new Parameter(ClassHelper.STRING_TYPE, "propertyName")
            final propertyNameParameterArray = [propertyNameParameter] as Parameter[]
            if(!classNode.getMethod(METHOD_NAME_HAS_CHANGED, propertyNameParameterArray )) {
                final containsMethodCallExpression = new MethodCallExpression(changeTrackingVariable, "contains", new VariableExpression(propertyNameParameter))
                containsMethodCallExpression.methodTarget = containsSetMethodNode
                final newMethod = classNode.addMethod(METHOD_NAME_HAS_CHANGED, PUBLIC, ClassHelper.boolean_TYPE, propertyNameParameterArray, null, new ReturnStatement(new BinaryExpression(changeTrackingVariable, Token.newSymbol(Types.LOGICAL_AND, 0, 0), containsMethodCallExpression)))
                final scope = new VariableScope()
                scope.putReferencedClassVariable(propertyNameParameter)
                newMethod.setVariableScope(scope)
            }

            // Implement the markDiry() method such that
            // void markDirty() { if( $changeProperties != null) $changeProperties << '$DIRTY_MARKER' }
            final markDirtyBody = new BlockStatement()

            final addMethodCall = new MethodCallExpression(changeTrackingVariable, "add", new ConstantExpression('$DIRTY_MARKER'))
            addMethodCall.setMethodTarget(addToSetMethodNode)
            final changeTrackingVariableNullCheck = new BooleanExpression(new BinaryExpression(changeTrackingVariable, Token.newSymbol(Types.COMPARE_NOT_EQUAL, 0, 0), new ConstantExpression(null)))
            def ifNotNullStatement = new IfStatement(changeTrackingVariableNullCheck, new ExpressionStatement(addMethodCall), new EmptyStatement())
            markDirtyBody.addStatement(ifNotNullStatement)
            if(!classNode.getMethod(METHOD_NAME_MARK_DIRTY, ZERO_PARAMETERS))
                classNode.addMethod(METHOD_NAME_MARK_DIRTY, PUBLIC, ClassHelper.VOID_TYPE, ZERO_PARAMETERS, null, markDirtyBody)

            // Implement the markDirty(String propertyName) method such that
            // void markDirty(String propertyName) { if( $changeProperties != null)  $changeProperties << propertyName }
            if(!classNode.getMethod(METHOD_NAME_MARK_DIRTY, propertyNameParameterArray)) {
                final markPropertyDirtyBody = new BlockStatement()
                final addPropertyMethodCall = new MethodCallExpression(changeTrackingVariable, "add", new VariableExpression(propertyNameParameter))
                addPropertyMethodCall.methodTarget = addToSetMethodNode
                ifNotNullStatement = new IfStatement(changeTrackingVariableNullCheck, new ExpressionStatement(addPropertyMethodCall), new EmptyStatement())
                markPropertyDirtyBody.addStatement(ifNotNullStatement)
                final newMethod = classNode.addMethod(METHOD_NAME_MARK_DIRTY, PUBLIC, ClassHelper.VOID_TYPE, propertyNameParameterArray, null, markPropertyDirtyBody)
                final scope = new VariableScope()
                scope.putReferencedClassVariable(propertyNameParameter)
                newMethod.setVariableScope(scope)

            }


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
                    final getterName = NameUtils.getGetterName(propertyName)
                    final propertyType = pn.getType().getPlainNodeReference()
                    classNode.addMethod(getterName, PUBLIC, propertyType, ZERO_PARAMETERS, null, new ReturnStatement(new VariableExpression(propertyField.getName())))

                    // now add the setter that tracks changes. Each setters becomes:
                    // void setFoo(String foo) { markDirty("foo"); this.foo = foo }
                    final setterName = NameUtils.getSetterName(propertyName)
                    final setterParameter = new Parameter(propertyType, propertyName)
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
            if(getterAndSetter.hasBoth()) {
                weaveIntoExistingSetter(propertyName, getterAndSetter, markDirtyMethodNode)
            }
        }
    }

    protected void weaveIntoExistingSetter(String propertyName, GetterAndSetter getterAndSetter, MethodNode markDirtyMethodNode) {
        final setterMethod = getterAndSetter.setter
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
