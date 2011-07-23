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
package org.grails.datastore.mapping.gemfire.config;

import org.grails.datastore.mapping.keyvalue.mapping.config.Family;

import com.gemstone.gemfire.cache.CacheListener;
import com.gemstone.gemfire.cache.CacheLoader;
import com.gemstone.gemfire.cache.CacheWriter;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.RegionAttributes;

/**
 * Configures a Gemfire region
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class Region extends Family {

    private RegionAttributes regionAttributes;
    private CacheListener[] cacheListeners;
    private CacheLoader cacheLoader;
    private CacheWriter cacheWriter;
    private DataPolicy dataPolicy;
    private String regionShortcut;

    public RegionAttributes getRegionAttributes() {
        return regionAttributes;
    }

    public String getRegionShortcut() {
        return regionShortcut;
    }

    public void setRegionShortcut(String regionShortcut) {
        this.regionShortcut = regionShortcut;
    }

    public void setRegion(String name) {
        setFamily(name);
    }

    public String getRegion() { return getFamily(); }

    public void setRegionAttributes(RegionAttributes regionAttributes) {
        this.regionAttributes = regionAttributes;
    }

    public CacheListener[] getCacheListeners() {
        return cacheListeners;
    }

    public void setCacheListeners(CacheListener[] cacheListeners) {
        this.cacheListeners = cacheListeners;
    }

    public CacheLoader getCacheLoader() {
        return cacheLoader;
    }

    public void setCacheLoader(CacheLoader cacheLoader) {
        this.cacheLoader = cacheLoader;
    }

    public CacheWriter getCacheWriter() {
        return cacheWriter;
    }

    public void setCacheWriter(CacheWriter cacheWriter) {
        this.cacheWriter = cacheWriter;
    }

    public DataPolicy getDataPolicy() {
        return dataPolicy;
    }

    public void setDataPolicy(DataPolicy dataPolicy) {
        this.dataPolicy = dataPolicy;
    }
}
