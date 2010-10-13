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
package org.springframework.datastore.mapping.gemfire;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.query.CqQuery;
import com.gemstone.gemfire.cache.query.IndexType;
import com.gemstone.gemfire.cache.query.QueryService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.RegionFactoryBean;
import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.keyvalue.mapping.KeyValue;
import org.springframework.datastore.mapping.model.*;
import org.springframework.datastore.mapping.model.types.BasicTypeConverterRegistrar;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Implementation of the {@link org.springframework.datastore.mapping.core.Datastore} interface
 * that maps entities into Gemfire regions
 *
 * @since 1.0
 * @author Graeme Rocher
 */
public class GemfireDatastore extends AbstractDatastore implements InitializingBean, DisposableBean, MappingContext.Listener {

    protected Cache gemfireCache;
    protected Map<PersistentEntity, GemfireTemplate> gemfireTemplates = new ConcurrentHashMap<PersistentEntity, GemfireTemplate>();
    protected Collection<CqQuery> continuousQueries = new ConcurrentLinkedQueue<CqQuery>();

    public static final String SETTING_CACHE_XML = "cacheXml";
    public static final String SETTING_PROPERTIES = "properties";

    public GemfireDatastore(MappingContext mappingContext, Map<String, String> connectionDetails) {
        super(mappingContext, connectionDetails != null ? connectionDetails : Collections.<String, String>emptyMap());

        mappingContext.addMappingContextListener(this);
    }

    protected void initializeRegions(Cache cache, MappingContext mappingContext) throws Exception {
        for (PersistentEntity entity : mappingContext.getPersistentEntities()) {
            initializeRegion(cache, entity);
            initializeIndices(cache, entity);
        }

    }

    protected void initializeIndices(Cache cache, PersistentEntity entity) throws Exception{
        final List<PersistentProperty> properties = entity.getPersistentProperties();

        final QueryService queryService = cache.getQueryService();
        String entityName = entity.getDecapitalizedName();
        final String idName = entity.getIdentity().getName();
        queryService.createIndex(entityName+"PrimaryKeyIndex", IndexType.PRIMARY_KEY, idName, "/"+entityName);
        for (PersistentProperty property : properties) {
            final boolean indexed = isIndexed(property) && Comparable.class.isAssignableFrom(property.getType());

            if(indexed) {
                queryService.createIndex(entityName + property.getCapitilizedName() + "Index", IndexType.FUNCTIONAL,property.getName(), "/"+entityName);
            }
        }
    }

    protected void initializeRegion(Cache cache, PersistentEntity entity) throws Exception {
        RegionFactoryBean regionFactory = new RegionFactoryBean();
        regionFactory.setCache(cache);

        regionFactory.setName(entity.getDecapitalizedName());
        regionFactory.setDataPolicy(DataPolicy.REPLICATE);
        regionFactory.afterPropertiesSet();
        final Region region = regionFactory.getObject();
        gemfireTemplates.put(entity, new GemfireTemplate(region));
    }

    public Cache getGemfireCache() {
        return gemfireCache;
    }

    public void addContinuousQuery(CqQuery query) {
        continuousQueries.add(query);
    }

    /**
     * Obtains the template used to query data for a particular entity
     *
     * @param entity The entity to use
     * @return The template
     */
    public GemfireTemplate getTemplate(PersistentEntity entity) {
        return gemfireTemplates.get(entity);
    }

    /**
     * Obtains the template used to query data for a particular entity
     *
     * @param entity The entity to use
     * @return The template
     */
    public GemfireTemplate getTemplate(Class entity) {
        final PersistentEntity e = getMappingContext().getPersistentEntity(entity.getName());
        if(e != null) {
            return gemfireTemplates.get(e);
        }
        return null;
    }

    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        return new GemfireSession(this, mappingContext);
    }


    public void destroy() throws Exception {
        if(gemfireCache != null) {
            gemfireCache.close();
            for (CqQuery continuousQuery : continuousQueries) {
                continuousQuery.close();
            }
            continuousQueries.clear();
        }
    }

    public void afterPropertiesSet() throws Exception {
        CacheFactoryBean cacheFactory = new CacheFactoryBean();
        if(connectionDetails != null) {
            if(connectionDetails.containsKey(SETTING_CACHE_XML)) {
                Object entry = connectionDetails.remove(SETTING_CACHE_XML);
                if(entry instanceof Resource) {
                    cacheFactory.setCacheXml((Resource) entry);
                }
                else {
                    cacheFactory.setCacheXml(new ClassPathResource(entry.toString()));
                }
            }

            if(connectionDetails.containsKey(SETTING_PROPERTIES)) {
                Object entry = connectionDetails.get(SETTING_PROPERTIES);
                if(entry instanceof Properties) {
                    cacheFactory.setProperties((Properties) entry);
                }
                else if(entry instanceof Map) {
                    final Properties props = new Properties();
                    props.putAll((Map)entry);
                    cacheFactory.setProperties(props);
                }
            }
        }

        try {
            cacheFactory.afterPropertiesSet();
            gemfireCache = cacheFactory.getObject();
            initializeRegions(gemfireCache, mappingContext);
            initializeConverters(mappingContext);
        } catch (Exception e) {
            throw new DatastoreConfigurationException("Failed to configure Gemfire cache and regions: " + e.getMessage(),e);
        }
    }

    public void persistentEntityAdded(PersistentEntity entity) {
        try {
            initializeRegion(gemfireCache, entity);
            initializeIndices(gemfireCache, entity);
        } catch (Exception e) {
            throw new DatastoreConfigurationException("Failed to configure Gemfire cache and regions for entity ["+entity+"]: " + e.getMessage(),e);
        }
    }
}
