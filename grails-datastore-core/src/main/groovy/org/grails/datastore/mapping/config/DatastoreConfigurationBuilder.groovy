package org.grails.datastore.mapping.config

import groovy.transform.CompileStatic
import org.springframework.core.env.PropertyResolver
import org.springframework.util.ReflectionUtils

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
abstract class DatastoreConfigurationBuilder<B, C> {
    final PropertyResolver propertyResolver
    final String configurationPrefix
    final String builderMethodPrefix

    /**
     * @param propertyResolver The property resolver
     * @param configurationPrefix The prefix to resolve settings from within the configuration. Example "grails.gorm.neo4j" or "grails.gorm.mongodb"
     * @param builderMethodPrefix The prefix to builder method calls. Default is null which results in builder methods like "foo(...)". Seting a prefix of "with" results in "withFoo(..)"
     *
     */
    DatastoreConfigurationBuilder(PropertyResolver propertyResolver, String configurationPrefix, String builderMethodPrefix = null) {
        this.propertyResolver = propertyResolver
        this.configurationPrefix = configurationPrefix
        this.builderMethodPrefix = builderMethodPrefix
    }


    C build() {
        B builder = createBuilder()
        buildInternal(builder, this.configurationPrefix)
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
        def builderClass = builder.getClass()
        def methods = builderClass.declaredMethods

        for (method in methods) {
            def methodName = method.name
            if(!Modifier.isPublic(method.modifiers)) {
                continue
            }
            def parameterTypes = method.parameterTypes

            if (parameterTypes.length == 1) {
                Class argType = parameterTypes[0]

                String settingName
                if(builderMethodPrefix != null && methodName.startsWith(builderMethodPrefix)) {
                    settingName = String.valueOf(Character.toLowerCase(methodName.charAt(4))) + methodName.substring(5)
                }
                else {
                    settingName = methodName
                }

                def value = propertyResolver.getProperty("${startingPrefix}.${settingName}", argType, null)
                if(value != null) {
                    ReflectionUtils.makeAccessible(method)
                    ReflectionUtils.invokeMethod(method, builder, value)
                }
            }
        }

        return toConfiguration(builder)
    }


}
