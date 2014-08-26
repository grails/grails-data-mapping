/* Copyright (C) 2010 SpringSource
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
package org.grails.datastore.gorm.neo4j

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.transaction.PlatformTransactionManager

/**
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
class Neo4jGormEnhancer extends GormEnhancer {

    public static final String UNDECLARED_PROPERTIES = "_neo4j_gorm_undecl_"

    Neo4jGormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager = null) {
        super(datastore, transactionManager)
    }

    @Override
    Neo4jDatastore getDatastore() {
        return super.getDatastore() as Neo4jDatastore
    }

    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        return new Neo4jGormStaticApi<D>(cls, datastore, finders, transactionManager)
    }

    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        final api = new Neo4jGormInstanceApi<D>(cls, datastore)
        api.failOnError = failOnError
        return api
    }

    public static Map<String, List<Object>> amendMapWithUndeclaredProperties(Map<String, Object> simpleProps, Object pojo, MappingContext mappingContext) {
        GroovyObject obj = (GroovyObject) pojo;
        Map<String, List<Object>> dynRelProps = [:].withDefault { [] }
        Map<String,Object> map = (Map) obj.getProperty(Neo4jGormEnhancer.UNDECLARED_PROPERTIES);
        if (map!=null) {
            for (Map.Entry<String,Object> entry : map.entrySet()) {
                if  (mappingContext.isPersistentEntity(entry.getValue())) {
                    dynRelProps[entry.getKey()] << entry.getValue()
                } else if (isCollectionWithPersistentEntities(entry.getValue(), mappingContext)) {
                    dynRelProps[entry.getKey()].addAll(entry.getValue())
                } else {
                    simpleProps.put(entry.getKey(), Neo4jUtils.mapToAllowedNeo4jType(entry.getValue(), mappingContext));
                }
            }
        }
        dynRelProps
    }

    static boolean isCollectionWithPersistentEntities(Object o, MappingContext mappingContext) {
        if (!(o instanceof Collection)) {
            return false;
        }
        return (!o.empty) && (o.every { mappingContext.isPersistentEntity(it) })
    }
}

class Neo4jGormInstanceApi<D> extends GormInstanceApi<D> {

    Neo4jGormInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
    }

    /**
     * Allows accessing to dynamic properties with the dot operator
     *
     * @param instance The instance
     * @param name The property name
     * @return The property value
     */
    def propertyMissing(D instance, String name) {

        def unwrappedInstance = unwrappedInstance(instance)

        MetaProperty mp = unwrappedInstance.hasProperty(Neo4jGormEnhancer.UNDECLARED_PROPERTIES);
        mp ? mp.getProperty(unwrappedInstance)[name] : null
    }

    /**
     * dealing with undeclared properties must not happen on proxied instances
     * @param instance
     * @return the unwrapped instance
     */
    private D unwrappedInstance(D instance) {
        def proxyFactory = datastore.mappingContext.proxyFactory
        proxyFactory.unwrap(instance)
    }

    /**
     * Allows setting a dynamic property via the dot operator
     * @param instance The instance
     * @param name The property name
     * @param val The value
     */
    def propertyMissing(D instance, String name, val) {

        def unwrappedInstance = unwrappedInstance(instance)

        if (name == Neo4jGormEnhancer.UNDECLARED_PROPERTIES) {
            unwrappedInstance.metaClass."${Neo4jGormEnhancer.UNDECLARED_PROPERTIES}" = val
        } else {
            MetaProperty mp = unwrappedInstance.hasProperty(Neo4jGormEnhancer.UNDECLARED_PROPERTIES);
            Map undeclaredProps
            if (mp) {
                undeclaredProps = mp.getProperty(unwrappedInstance)
            } else {
                undeclaredProps = [:]
                unwrappedInstance.metaClass."${Neo4jGormEnhancer.UNDECLARED_PROPERTIES}" = undeclaredProps
            }
            (val == null) ? undeclaredProps.remove(name) : undeclaredProps.put(name, val)
            if (datastore.mappingContext.isPersistentEntity(val)) {
                val.save()
            } else if (Neo4jGormEnhancer.isCollectionWithPersistentEntities(val, datastore.mappingContext)) {
                val.each { it.save() }
            }
            if (unwrappedInstance instanceof DirtyCheckable) {
                ((DirtyCheckable)unwrappedInstance).markDirty(name)
            }
        }
    }

    /**
     * Allows subscript access to schemaless attributes.
     *
     * @param instance The instance
     * @param name The name of the field
     */
    void putAt(D instance, String name, value) {
        instance."$name" = value

    }

    /**
     * Allows subscript access to schemaless attributes.
     *
     * @param instance The instance
     * @param name The name of the field
     * @return the value
     */
    def getAt(D instance, String name) {
        instance."$name"
    }

    /**
     * perform a cypher query
     * @param queryString
     * @param params
     * @return
     */
    def cypher(instance, String queryString, Map params ) {
        params['this'] = instance.id
        ((Neo4jDatastore)datastore).cypherEngine.execute(queryString, params)
    }

    def cypher(instance, String queryString, List params ) {
        params.add(0, instance.id)
        ((Neo4jDatastore)datastore).cypherEngine.execute(queryString, params)
    }

    def cypher(instance, String queryString) {
        ((Neo4jDatastore)datastore).cypherEngine.execute(queryString, Collections.singletonList(instance.id))
    }

}

class Neo4jGormStaticApi<D> extends GormStaticApi<D> {

    Neo4jGormStaticApi(Class<D> persistentClass, Neo4jDatastore datastore, List<FinderMethod> finders, PlatformTransactionManager transactionManager) {
        super(persistentClass, datastore, finders, transactionManager)
    }

    def cypherStatic(String queryString, Map params) {
        ((Neo4jDatastore)datastore).cypherEngine.execute(queryString, params)
    }

    def cypherStatic(String queryString, List params) {
        ((Neo4jDatastore)datastore).cypherEngine.execute(queryString, params)
    }

    def cypherStatic(String queryString) {
        ((Neo4jDatastore)datastore).cypherEngine.execute(queryString)
    }

}
