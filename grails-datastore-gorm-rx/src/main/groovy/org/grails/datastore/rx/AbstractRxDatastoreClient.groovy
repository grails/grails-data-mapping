package org.grails.datastore.rx

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.core.env.PropertyResolver

/**
 * Abstract implementation the {@link RxDatastoreClient} interface
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
abstract class AbstractRxDatastoreClient<T> implements RxDatastoreClient<T> {

    protected final MappingContext mappingContext

    AbstractRxDatastoreClient(MappingContext mappingContext) {
        this.mappingContext = mappingContext
    }
}
