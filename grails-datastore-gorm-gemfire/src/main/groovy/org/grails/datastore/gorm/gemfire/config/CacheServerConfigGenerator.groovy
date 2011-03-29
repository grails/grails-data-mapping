package org.grails.datastore.gorm.gemfire.config

import org.springframework.datastore.mapping.gemfire.GemfireDatastore
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.gemfire.config.Region

/**
 * Generates a Gemfire cache.xml configuration for the Datastore
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
