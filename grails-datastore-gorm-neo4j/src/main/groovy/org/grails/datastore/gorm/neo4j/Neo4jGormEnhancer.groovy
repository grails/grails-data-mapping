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
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.transaction.PlatformTransactionManager

/**
 * Additional enhancements to GORM for Neo4j
 *
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 * @author Graeme Rocher
 */
@CompileStatic
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
        Map<String,Object> map = (Map) obj.getProperty(UNDECLARED_PROPERTIES);
        if (map!=null) {
            for (Map.Entry<String,Object> entry : map.entrySet()) {
                def value = entry.getValue()
                def key = entry.getKey()

                if  (mappingContext.isPersistentEntity(value)) {
                    dynRelProps[key] << value
                } else if (isCollectionWithPersistentEntities(value, mappingContext)) {
                    dynRelProps[key].addAll((Collection)value)
                } else {
                    simpleProps.put(key, ((Neo4jMappingContext)mappingContext).convertToNative(entry.value))
                }
            }
        }
        return dynRelProps
    }

    static boolean isCollectionWithPersistentEntities(Object o, MappingContext mappingContext) {
        if (!(o instanceof Collection)) {
            return false;
        }
        else {
            Collection c = (Collection)o
            return (!c.empty) && (c.any() { mappingContext.isPersistentEntity(it) })
        }
    }
}

