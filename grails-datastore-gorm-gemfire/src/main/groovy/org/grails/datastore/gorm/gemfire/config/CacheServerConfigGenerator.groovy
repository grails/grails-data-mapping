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
package org.grails.datastore.gorm.gemfire.config

import org.grails.datastore.mapping.gemfire.GemfireDatastore
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.gemfire.config.Region

/**
 * Generates a Gemfire cache.xml configuration for the Datastore.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class CacheServerConfigGenerator {

    GemfireDatastore datastore

    CacheServerConfigGenerator(GemfireDatastore datastore) {
        this.datastore = datastore
    }

    void generate(File target) {
        def regions = []
        for (PersistentEntity entity in datastore.mappingContext.persistentEntities) {
            Region r = entity.mapping.mappedForm
            def regionName = r?.region ?: entity.decapitalizedName
            regions << """\
<region name="${regionName}">
<region-attributes>
        <partition-attributes redundant-copies="1" />
</region-attributes>
</region>
            """
        }
        target.withWriter { w ->
            w << """\
<?xml version="1.0"?>
<!DOCTYPE cache PUBLIC
        "-//GemStone Systems, Inc.//GemFire Declarative Caching 6.5//EN"
        "http://www.gemstone.com/dtd/cache6_5.dtd">

<cache>
    ${regions.join(System.getProperty('line.separator'))}
</cache>
            """
        }
    }
}
