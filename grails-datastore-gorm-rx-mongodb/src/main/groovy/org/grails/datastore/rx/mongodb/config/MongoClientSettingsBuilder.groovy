package org.grails.datastore.rx.mongodb.config

import com.mongodb.ConnectionString
import com.mongodb.MongoCredential
import com.mongodb.async.client.MongoClientSettings
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.mongo.MongoConstants
import org.springframework.core.env.PropertyResolver
import org.springframework.util.ReflectionUtils

import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Helper class for building MongoClientSettings from a {@link PropertyResolver}
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
@Slf4j
class MongoClientSettingsBuilder {

    final PropertyResolver propertyResolver
    final String databaseName

    private String prefix = MongoConstants.SETTING_OPTIONS

    private ConnectionString connectionString
    private String host
    private String username
    private String password
    private String uAndP
    private MongoCredential mongoCredential

    MongoClientSettingsBuilder(PropertyResolver propertyResolver) {
        this(propertyResolver,  propertyResolver.getProperty(MongoConstants.SETTING_DATABASE_NAME, 'test'))
    }

    MongoClientSettingsBuilder(PropertyResolver propertyResolver, String databaseName) {
        this.propertyResolver = propertyResolver
        this.databaseName = databaseName

        host = propertyResolver.getProperty(MongoConstants.SETTING_HOST, '')
        username = propertyResolver.getProperty(MongoConstants.SETTING_USERNAME, '')
        password = propertyResolver.getProperty(MongoConstants.SETTING_PASSWORD, '')
        uAndP = username && password ? "$username:$password@" : ''
        if(host) {
            def port = propertyResolver.getProperty(MongoConstants.SETTING_PORT, '')
             port = port ? ":$port" : ''
            connectionString = new ConnectionString("mongodb://${uAndP}${host}${port}/$databaseName")
        }
        else {
            connectionString = new ConnectionString(propertyResolver.getProperty(MongoConstants.SETTING_CONNECTION_STRING, propertyResolver.getProperty(MongoConstants.SETTING_URL, "mongodb://localhost/$databaseName")))
        }
        mongoCredential = uAndP ? MongoCredential.createCredential(username, databaseName, password.toCharArray()) : null
    }

    MongoClientSettings build() {
        MongoClientSettings.Builder builder = MongoClientSettings.builder()
        (MongoClientSettings)buildInternal(builder, prefix)
    }

    private Object buildInternal(Object builder, String startingPrefix) {
        def builderClass = builder.getClass()
        def methods = builderClass.declaredMethods

        def applyConnectionStringMethod = ReflectionUtils.findMethod(builderClass, 'applyConnectionString', ConnectionString)
        if(applyConnectionStringMethod != null) {
            applyConnectionStringMethod.invoke(builder, connectionString)
        }

        if(mongoCredential != null) {
            def credentialListMethod = ReflectionUtils.findMethod(builderClass, 'credentialList', List)
            if(credentialListMethod != null) {
                credentialListMethod.invoke(builder, Arrays.asList(mongoCredential))
            }
        }

        for (method in methods) {
            def methodName = method.name
            if(!Modifier.isPublic(method.modifiers) || methodName.equals('applyConnectionString') || methodName.equals('credentialList')) {
                continue
            }

            def parameterTypes = method.parameterTypes
            if (parameterTypes.length == 1) {
                Class argType = parameterTypes[0]

                def builderMethod = ReflectionUtils.findMethod(argType, 'builder')
                String propertyPath = "${startingPrefix}.${ methodName}"
                if (builderMethod != null && Modifier.isStatic(builderMethod.modifiers)) {

                    def newBuilder = builderMethod.invoke(argType)
                    if(methodName == 'clusterSettings') {
                        applyConnectionString(newBuilder, connectionString)
                    }
                    method.invoke(builder, buildInternal(newBuilder, propertyPath))

                } else {

                    if(argType.isEnum()) {
                        def value = propertyResolver.getProperty(propertyPath, "")
                        if (value) {
                            try {
                                method.invoke(builder, Enum.valueOf((Class)argType, value))
                            } catch (Throwable e) {
                                log.warn("Error occurred reading setting [$propertyPath]: ${e.message}", e)
                            }
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
                        } catch (e) {
                            log.warn("Error occurred reading setting [$propertyPath]: ${e.message}", e)
                        }
                    }
                    else if(!List.isAssignableFrom(argType)){
                        try {
                            def value = propertyResolver.getProperty(propertyPath, argType, null)
                            if(value != null) {
                                method.invoke(builder, value)
                            }
                        } catch (Throwable e) {
                            log.warn("Error occurred reading setting [$propertyPath]: ${e.message}", e)
                        }
                    }
                }
            }

        }

        return doBuild(builder)
    }

    @CompileDynamic
    private void applyConnectionString(newBuilder, ConnectionString connectionString) {
        newBuilder.applyConnectionString(connectionString)
    }

    @CompileDynamic
    private Object doBuild(builder) {
        builder.build()
    }
}
