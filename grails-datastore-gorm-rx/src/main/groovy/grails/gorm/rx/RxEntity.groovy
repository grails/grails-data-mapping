package grails.gorm.rx

import grails.gorm.rx.api.RxGormOperations
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.validation.ValidationErrors
import org.grails.gorm.rx.api.RxGormEnhancer
import org.grails.gorm.rx.api.RxGormInstanceApi
import org.grails.gorm.rx.api.RxGormStaticApi
import org.springframework.validation.Errors
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
trait RxEntity<D> implements RxGormOperations<D>, DirtyCheckable {

    /**
     * The validation errors object
     */
    Errors errors
    /**
     * Obtains the errors for an instance
     * @return The {@link Errors} instance
     */
    Errors getErrors() {
        if(errors == null) {
            errors = new ValidationErrors(this)
        }
        errors
    }

    /**
     * Clears any errors that exist on an instance
     */
    void clearErrors() {
        errors = new ValidationErrors(this)
    }

    /**
     * Tests whether an instance has any errors
     * @return True if errors exist
     */
    Boolean hasErrors() {
        getErrors().hasErrors()
    }

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

    /**
     * List all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<D> findAll() {
        list()
    }

    /**
     * List all entities and return an observable
     *
     * @return An observable with all results
     */
    static Observable<D> findAll(Map args) {
        list(args)
    }

    /**
     * Handles dynamic finders
     *
     * @param methodName The method name
     * @param arg The argument to the method
     *
     * @return An observable with the result
     */
    static Observable<D> staticMethodMissing(String methodName, arg) {
        currentRxGormStaticApi().methodMissing(methodName, arg)
    }

    private RxGormInstanceApi<D> currentRxGormInstanceApi() {
        (RxGormInstanceApi<D>)RxGormEnhancer.findInstanceApi(this.getClass())
    }

    private static RxGormStaticApi<D> currentRxGormStaticApi() {
        (RxGormStaticApi<D>)RxGormEnhancer.findStaticApi(this)
    }
}