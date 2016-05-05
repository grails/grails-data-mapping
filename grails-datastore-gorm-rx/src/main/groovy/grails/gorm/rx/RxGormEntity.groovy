package grails.gorm.rx

import grails.gorm.rx.api.RxGormOperations
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.gorm.rx.api.RxGormEnhancer
import org.grails.gorm.rx.api.RxGormInstanceApi
import org.grails.gorm.rx.api.RxGormStaticApi
import rx.Observable
import rx.Single

/**
 * Represents a reactive GORM entity
 *
 * @author Graeme Rocher
 * @since 6.0
 *
 * @param <D> The entity type
 */
@CompileStatic
trait RxGormEntity<D> implements RxGormOperations<D>, DirtyCheckable {

    /**
     * Save an instance and return an observable
     *
     * @return An observable
     */
    Observable<D> save() {
        save(Collections.<String,Object>emptyMap())
    }

    /**
     * Save an instance and return an observable
     *
     * @return An observable
     */
    Observable<D> save(Map<String, Object> arguments) {
        currentRxGormInstanceApi().save(this, arguments)
    }

    /**
     * Retrieve an instance by id
     *
     * @param id The id of the instance
     * @return An observable
     */
    static Observable<D> get(Serializable id) {
        currentRxGormStaticApi().get(id)
    }

    /**
     * List all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<D> list() {
        currentRxGormStaticApi().list()
    }

    /**
     * List all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<D> list(Map args) {
        currentRxGormStaticApi().list(args)
    }

    static Observable $static_methodMissing() {

    }

    private RxGormInstanceApi<D> currentRxGormInstanceApi() {
        (RxGormInstanceApi<D>)RxGormEnhancer.findInstanceApi(this.getClass())
    }

    private static RxGormStaticApi<D> currentRxGormStaticApi() {
        (RxGormStaticApi<D>)RxGormEnhancer.findStaticApi(this)
    }
}