package org.grails.datastore.rx.query

import groovy.transform.CompileStatic
import rx.Observable

/**
 * Represents a reactive query implementation in RxGORM
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
interface RxQuery<T> {


    /**
     * @return All results matching this query as an observable
     */
    Observable<T> findAll()

    /**
     *
     * @return A single result matching this query as an observable
     */

    Observable<T> singleResult()
}
