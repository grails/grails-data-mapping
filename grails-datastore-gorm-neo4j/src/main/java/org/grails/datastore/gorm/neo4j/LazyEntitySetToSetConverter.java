package org.grails.datastore.gorm.neo4j;

import org.springframework.core.convert.converter.Converter;

import java.util.Set;

/**
 * bypass the default CollectionToCollectionConverter in order to not expand proxies
*/
public class LazyEntitySetToSetConverter implements Converter<LazyEnititySet, Set> {

    @Override
    public Set convert(LazyEnititySet source) {
        return source;
    }
}
