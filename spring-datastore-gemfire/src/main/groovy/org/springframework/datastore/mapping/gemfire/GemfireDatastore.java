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

import com.gemstone.gemfire.cache.*;
import com.gemstone.gemfire.cache.client.Pool;
import com.gemstone.gemfire.cache.query.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.RegionFactoryBean;
import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.model.*;

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
    protected Pool gemfirePool;
    protected Map<PersistentEntity, GemfireTemplate> gemfireTemplates = new ConcurrentHashMap<PersistentEntity, GemfireTemplate>();
    protected Collection<CqQuery> continuousQueries = new ConcurrentLinkedQueue<CqQuery>();

    public static final String SETTING_CACHE_XML = "cacheXml";
    public static final String SETTING_PROPERTIES = "properties";

    public GemfireDatastore(MappingContext mappingContext, Map<String, String> connectionDetails) {
        super(mappingContext, connectionDetails != null ? connectionDetails : Collections.<String, String>emptyMap());

        mappingContext.addMappingContextListener(this);
    }

    public GemfireDatastore(MappingContext mappingContext) {
        this(mappingContext, Collections.<String, String>emptyMap());
    }

    public GemfireDatastore(MappingContext mappingContext, Cache gemfireCache) {
        this(mappingContext, Collections.<String, String>emptyMap());
        this.gemfireCache = gemfireCache;
    }

    public void setGemfirePool(Pool gemfirePool) {
        this.gemfirePool = gemfirePool;
    }

    public Pool getGemfirePool() {
        return gemfirePool;
    }

    protected void initializeRegions(Cache cache, MappingContext mappingContext) throws Exception {
        for (PersistentEntity entity : mappingContext.getPersistentEntities()) {
            Region region = initializeRegion(cache, entity);
            initializeIndices(cache, entity, region);
        }

    }

    protected void initializeIndices(Cache cache, PersistentEntity entity, Region region) throws Exception {
        final List<PersistentProperty> properties = entity.getPersistentProperties();

        final QueryService queryService = cache.getQueryService();
        String entityName = entity.getDecapitalizedName();
        final String idName = entity.getIdentity().getName();

        org.springframework.datastore.mapping.gemfire.config.Region mappedRegion = getMappedRegionInfo(entity);

        final Collection<Index> indices = queryService.getIndexes(region);
        final String indexName = entityName + "PrimaryKeyIndex";

        if(!checkIndexExists(indices, indexName)) {
            try {
                queryService.createIndex(indexName, IndexType.PRIMARY_KEY, idName, mappedRegion != null && mappedRegion.getRegion() != null ?
                        "/" + mappedRegion.getRegion() : "/"+entityName);
            }
            catch (IndexExistsException e) {
               // ignore
            }
            catch(IndexNameConflictException e) {
                // ignore
            }
        }
        for (PersistentProperty property : properties) {
            final boolean indexed = isIndexed(property) && Comparable.class.isAssignableFrom(property.getType());

            if(indexed) {
                final String propertyIndexName = entityName + property.getCapitilizedName() + "Index";
                if(!checkIndexExists(indices, propertyIndexName)) {
                    try {
                        queryService.createIndex(propertyIndexName, IndexType.FUNCTIONAL,property.getName(), "/"+entityName);
                    }
                    catch (IndexExistsException e) {
                       // ignore
                    }
                    catch(IndexNameConflictException e) {
                        // ignore
                    }
                }
            }
        }
    }

    private boolean checkIndexExists(Collection<Index> indices, String indexName) {
        boolean indexExists = false;
        if(indices != null) {
            for (Index index : indices) {
                if(index.getName().equals(indexName)) {
                    indexExists = true;
                }
            }
        }
        return indexExists;
    }

    protected Region initializeRegion(Cache cache, PersistentEntity entity) throws Exception {


        org.springframework.datastore.mapping.gemfire.config.Region mappedRegion = getMappedRegionInfo(entity);

        final boolean hasMappedRegion = mappedRegion != null;
        String regionName;
        if(hasMappedRegion && mappedRegion.getRegion() != null) {
            regionName = mappedRegion.getRegion();

        }
        else {
            regionName = entity.getDecapitalizedName();
        }

        Region region = cache.getRegion(regionName);

        if(region == null) {
            RegionFactoryBean regionFactory = new RegionFactoryBean();
            regionFactory.setCache(cache);

            regionFactory.setName(regionName);
            if(gemfirePool != null) {
                AttributesFactory factory = new AttributesFactory();
                factory.setScope(Scope.LOCAL);
                factory.setPoolName(gemfirePool.getName());
                regionFactory.setAttributes(factory.create());
            }
            else {
                if(hasMappedRegion && mappedRegion.getDataPolicy() != null) {
                    regionFactory.setDataPolicy(mappedRegion.getDataPolicy());
                }
                else {
                    regionFactory.setDataPolicy(DataPolicy.PARTITION);
                }
                if(hasMappedRegion && mappedRegion.getRegionAttributes() != null) {
                    regionFactory.setAttributes(mappedRegion.getRegionAttributes());
                }
                if(hasMappedRegion && mappedRegion.getCacheListeners() != null) {
                    regionFactory.setCacheListeners(mappedRegion.getCacheListeners());
                }
                if(hasMappedRegion && mappedRegion.getCacheLoader() != null) {
                    regionFactory.setCacheLoader(mappedRegion.getCacheLoader());
                }
                if(hasMappedRegion && mappedRegion.getCacheWriter() != null) {
                    regionFactory.setCacheWriter(mappedRegion.getCacheWriter());
                }
            }


            regionFactory.afterPropertiesSet();
            region = regionFactory.getObject();
			if(gemfirePool != null) {
				region.registerInterest("ALL_KEYS");
			}
        }

        gemfireTemplates.put(entity, new GemfireTemplate(region) /*{
            @Override
            public <T> T execute(GemfireCallback<T> action) throws DataAccessException {
                long now = System.currentTimeMillis();
                try {
                    return super.execute(action);
                } finally {
                    System.out.println("Gemfire query took " + (System.currentTimeMillis() - now) + "ms");
                }
            }


        }*/);
        return region;
    }

    private org.springframework.datastore.mapping.gemfire.config.Region getMappedRegionInfo(PersistentEntity entity) {
        final Object mappedForm = entity.getMapping().getMappedForm();

        org.springframework.datastore.mapping.gemfire.config.Region mappedRegion = null;
        if(mappedForm instanceof org.springframework.datastore.mapping.gemfire.config.Region)
            mappedRegion = (org.springframework.datastore.mapping.gemfire.config.Region) mappedForm;
        return mappedRegion;
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

            if(gemfireCache == null) {
                cacheFactory.afterPropertiesSet();
                gemfireCache = cacheFactory.getObject();
            }
            initializeRegions(gemfireCache, mappingContext);
            initializeConverters(mappingContext);
        } catch (Exception e) {
            throw new DatastoreConfigurationException("Failed to configure Gemfire cache and regions: " + e.getMessage(),e);
        }
    }

    public void persistentEntityAdded(PersistentEntity entity) {
        try {
            Region region = initializeRegion(gemfireCache, entity);
            initializeIndices(gemfireCache, entity, region);
        } catch (Exception e) {
            throw new DatastoreConfigurationException("Failed to configure Gemfire cache and regions for entity ["+entity+"]: " + e.getMessage(),e);
        }
    }
}
