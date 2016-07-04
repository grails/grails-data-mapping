package org.grails.datastore.mapping.config

import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.core.exceptions.ConfigurationException
import org.grails.datastore.mapping.reflect.NameUtils
import org.springframework.core.env.PropertyResolver
import org.springframework.util.ReflectionUtils

import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * A generic configuration builder that implementors can implement to construct the configuration from the source {@link PropertyResolver}
 *
 * @param <B> The builder type (examples are MongoClientSettings.Builder or Neo4j Bolt's Config.ConfigBuilder
 * @param <C> The finalized configuration constructions from the builder (examples are MongoClientSettings or Neo4j Bolt's Config)
 *
 * @author Graeme Rocher
 */
@CompileStatic
@Slf4j
abstract class ConfigurationBuilder<B, C> {
    private static final Set<String> IGNORE_METHODS = ['seProperty', 'propertyMissing'] as Set
    final PropertyResolver propertyResolver
    final String configurationPrefix
    final String builderMethodPrefix
    final C fallBackConfiguration
    protected B rootBuilder

    /**
     * @param propertyResolver The property resolver
     * @param configurationPrefix The prefix to resolve settings from within the configuration. Example "grails.gorm.neo4j" or "grails.gorm.mongodb"
     * @param builderMethodPrefix The prefix to builder method calls. Default is null which results in builder methods like "foo(...)". Seting a prefix of "with" results in "withFoo(..)"
     *
     */
    ConfigurationBuilder(PropertyResolver propertyResolver, String configurationPrefix, String builderMethodPrefix) {
        this.propertyResolver = propertyResolver
        this.configurationPrefix = configurationPrefix
        this.builderMethodPrefix = builderMethodPrefix
        this.fallBackConfiguration = null
    }

    /**
     * @param propertyResolver The property resolver
     * @param configurationPrefix The prefix to resolve settings from within the configuration. Example "grails.gorm.neo4j" or "grails.gorm.mongodb"
     * @param builderMethodPrefix The prefix to builder method calls. Default is null which results in builder methods like "foo(...)". Seting a prefix of "with" results in "withFoo(..)"
     * @param fallBackConfiguration An object to read the fallback configuration from
     */
    ConfigurationBuilder(PropertyResolver propertyResolver, String configurationPrefix, C fallBackConfiguration = null, String builderMethodPrefix = null) {
        this.propertyResolver = propertyResolver
        this.configurationPrefix = configurationPrefix
        this.builderMethodPrefix = builderMethodPrefix
        this.fallBackConfiguration = fallBackConfiguration
    }


    C build() {
        rootBuilder = createBuilder()
        buildInternal(rootBuilder, this.configurationPrefix)
    }

    /**
     * Creates the native builder
     *
     * @return The native builder
     */
    protected abstract B createBuilder()

    /**
     * Convert the builder to the final configuration
     *
     * @param builder The builder
     * @return The final configuration
     */
    protected abstract C toConfiguration(B builder)

    private C buildInternal(B builder, String startingPrefix) {
        buildRecurse(builder, this.fallBackConfiguration, startingPrefix)

        return toConfiguration(builder)
    }

    private List<Class> toHierarchy(Class cls) {
        List<Class> classes = [cls]
        while(cls != Object) {
            def superClass = cls.getSuperclass()
            if(superClass == Object) break

            classes.add(superClass)
            cls = superClass
        }
        return classes.reverse()
    }

    protected void buildRecurse(Object builder, Object fallBackConfig, String startingPrefix) {

        List<Class> hierarchy = toHierarchy(builder.getClass())

        startBuild(builder, startingPrefix)

        for(Class builderClass in hierarchy) {

            def methods = builderClass.declaredMethods
            for (method in methods) {
                def methodName = method.name
                if (!Modifier.isPublic(method.modifiers) || method.isSynthetic() || IGNORE_METHODS.contains(methodName)) {
                    continue
                }
                def parameterTypes = method.parameterTypes

                if (parameterTypes.length == 1) {
                    Class argType = parameterTypes[0]
                    String settingName

                    boolean hasBuilderPrefix = builderMethodPrefix != null
                    if (hasBuilderPrefix && methodName.startsWith(builderMethodPrefix)) {
                        settingName = String.valueOf(Character.toLowerCase(methodName.charAt(4))) + methodName.substring(5)
                    }
                    else if(hasBuilderPrefix) {
                        continue
                    }
                    else if(!hasBuilderPrefix && (org.grails.datastore.mapping.reflect.ReflectionUtils.isGetter(methodName, parameterTypes) ||
                            org.grails.datastore.mapping.reflect.ReflectionUtils.isSetter(methodName, parameterTypes))) {
                        // don't process getters by default
                        continue
                    }
                    else {
                        settingName = methodName
                    }

                    def builderMethod = ReflectionUtils.findMethod(argType, 'builder')
                    String propertyPath = "${startingPrefix}.${settingName}"
                    if (builderMethod != null && Modifier.isStatic(builderMethod.modifiers)) {
                        Method existingGetter = ReflectionUtils.findMethod(builderClass, NameUtils.getGetterName(methodName))
                        def newBuilder

                        if( existingGetter != null ) {
                            newBuilder = existingGetter.invoke(builder)
                        }
                        if(newBuilder == null) {
                            newBuilder = builderMethod.invoke(argType)
                        }

                        newChildBuilder(newBuilder, propertyPath)

                        Object fallBackChildConfig = getFallBackValue(fallBackConfig, methodName)
                        buildRecurse(newBuilder, fallBackChildConfig, propertyPath)

                        def buildMethod = ReflectionUtils.findMethod(newBuilder.getClass(), 'build')
                        if(buildMethod != null) {
                            method.invoke(builder, buildMethod.invoke(newBuilder))
                        }
                        else {
                            method.invoke(builder, newBuilder )
                        }
                        continue
                    }

                    def buildMethod = ReflectionUtils.findMethod(argType, 'build')
                    if(buildMethod != null) {
                        Method existingGetter = ReflectionUtils.findMethod(builderClass, NameUtils.getGetterName(methodName))
                        def newBuilder

                        if( existingGetter != null ) {
                            newBuilder = existingGetter.invoke(builder)

                            if(newBuilder != null) {
                                Object fallBackChildConfig = getFallBackValue(fallBackConfig, methodName)
                                newBuilder = newChildBuilderForFallback(newBuilder, fallBackChildConfig)
                                buildRecurse(newBuilder, fallBackChildConfig, propertyPath)
                                newChildBuilder(newBuilder, propertyPath)
                                method.invoke(builder, newBuilder )
                                continue
                            }
                        }
                    }

                    Builder builderAnnotation = argType.getAnnotation(Builder)
                    if(builderAnnotation != null && builderAnnotation.builderStrategy() == SimpleStrategy) {
                        Method existingGetter = ReflectionUtils.findMethod(builderClass, NameUtils.getGetterName(methodName))
                        def newBuilder
                        if( existingGetter != null ) {
                            newBuilder = existingGetter.invoke(builder)
                        }
                        if(newBuilder == null) {
                            newBuilder = argType.newInstance()
                        }

                        newChildBuilder(newBuilder, propertyPath)

                        Object fallBackChildConfig = getFallBackValue(fallBackConfig, methodName)
                        buildRecurse(newBuilder,fallBackChildConfig, propertyPath)
                        method.invoke(builder, newBuilder)
                        continue
                    }

                    if(ConfigurationBuilder.isAssignableFrom(argType)) {
                        try {
                            Method existingGetter = ReflectionUtils.findMethod(builderClass, NameUtils.getGetterName(methodName))
                            ConfigurationBuilder newBuilder
                            if( existingGetter != null ) {
                                newBuilder = (ConfigurationBuilder)existingGetter.invoke(builder)
                            }
                            if(newBuilder == null) {
                                newBuilder = (ConfigurationBuilder)argType.newInstance(this.propertyResolver, propertyPath)

                            }
                            newChildBuilder(newBuilder, propertyPath)
                            method.invoke(builder, newBuilder)
                        } catch (Throwable e) {
                            throw new ConfigurationException("Cannot read configuration for path $propertyPath: $e.message", e)
                        }
                        continue
                    }

                    def valueOfMethod = ReflectionUtils.findMethod(argType, 'valueOf')
                    if (valueOfMethod != null && Modifier.isStatic(valueOfMethod.modifiers)) {
                        try {
                            def value = propertyResolver.getProperty(propertyPath, "")
                            if(value) {
                                def converted = valueOfMethod.invoke(argType, value)
                                method.invoke(builder, converted)
                            }
                        } catch (Throwable e) {
                            throw new ConfigurationException("Cannot read configuration for path $propertyPath: $e.message", e)
                        }
                    }
                    else {
                        Object fallBackValue = getFallBackValue(fallBackConfig, methodName)

                        def value = propertyResolver.getProperty("${startingPrefix}.${settingName}", argType, fallBackValue)
                        if (value != null) {
                            ReflectionUtils.makeAccessible(method)
                            ReflectionUtils.invokeMethod(method, builder, value)
                        }
                    }

                } else if (methodName.startsWith("get") && parameterTypes.length == 0) {
                    if (method.returnType.getAnnotation(Builder)) {
                        def childBuilder = method.invoke(builder)
                        if(childBuilder != null) {
                            Object fallBackChildConfig = null
                            if (fallBackConfig != null) {
                                Method fallbackGetter = ReflectionUtils.findMethod(fallBackConfig.getClass(), methodName)
                                if (fallbackGetter != null) {
                                    fallBackChildConfig = fallbackGetter.invoke(fallBackConfig)
                                }
                            }
                            buildRecurse(childBuilder, fallBackChildConfig, "${startingPrefix}.${NameUtils.getPropertyNameForGetterOrSetter(methodName)}")
                        }
                    }
                }
            }

        }

    }

    protected Object newChildBuilderForFallback(Object childBuilder, Object fallbackConfig) {
        return childBuilder
    }

    private Object getFallBackValue(fallBackConfig, String methodName) {
        Object fallBackValue = null
        if (fallBackConfig != null) {
            Method fallbackGetter = ReflectionUtils.findMethod(fallBackConfig.getClass(), NameUtils.getGetterName(methodName))
            if (fallbackGetter != null) {
                fallBackValue = fallbackGetter.invoke(fallBackConfig)
            }
        }
        return fallBackValue
    }

    /**
     * Subclasses can override for when a new child builder is created
     *
     * @param builder The builder
     * @param configurationPath The configuration path
     */
    protected void newChildBuilder(Object builder, String configurationPath) {
        // no-op
    }

    /**
     * Subclasses can override for when building starts for the given builder
     *
     * @param builder The current builder
     * @param configurationPath The configuration path
     */
    protected void startBuild(Object builder, String configurationPath) {
        // no-op
    }
}
