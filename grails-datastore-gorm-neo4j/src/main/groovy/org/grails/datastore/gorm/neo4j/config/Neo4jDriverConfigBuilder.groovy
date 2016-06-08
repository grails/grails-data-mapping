package org.grails.datastore.gorm.neo4j.config

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.neo4j.Neo4jDatastore
import org.neo4j.driver.v1.Config
import org.springframework.core.env.PropertyResolver
import org.springframework.util.ReflectionUtils

import java.lang.reflect.Modifier

/**
 * Constructs the Neo4j driver configuration
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
@Slf4j
class Neo4jDriverConfigBuilder {
    final PropertyResolver propertyResolver
    private String prefix = Neo4jDatastore.SETTING_NEO4J_DB_PROPERTIES

    Neo4jDriverConfigBuilder(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver
    }

    Config build() {
        Config.ConfigBuilder builder = Config.build()
        buildInternal(builder, prefix)
    }

    private Config buildInternal(Config.ConfigBuilder builder, String startingPrefix) {
        def builderClass = builder.getClass()
        def methods = builderClass.declaredMethods

        for (method in methods) {
            def methodName = method.name
            if(!Modifier.isPublic(method.modifiers)) {
                continue
            }

            if(methodName.startsWith('with')) {

                def parameterTypes = method.parameterTypes
                if (parameterTypes.length == 1) {
                    Class argType = parameterTypes[0]

                    String settingName = String.valueOf(Character.toLowerCase(methodName.charAt(5))) + methodName.substring(5)

                    def value = propertyResolver.getProperty("${startingPrefix}.${settingName}", argType, null)
                    if(value != null) {
                        ReflectionUtils.makeAccessible(method)
                        ReflectionUtils.invokeMethod(method, builder, value)
                    }
                }
            }

        }

        return builder.toConfig()
    }

}
