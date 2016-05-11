package grails.gorm.rx.api

import rx.Observable
import rx.Single

/**
 * Interface for Reactive GORM operations on instances
 *
 * @author Graeme Rocher
 * @since 6.0
 */
interface RxGormOperations<D> {

    /**
     * Saves an entity and returns a {@link Single}
     *
     * @return A {@link Single} with the result of the operation
     */
    Observable<D> save()

    /**
     * Saves an entity and returns a {@link Single}
     *
     * @param arguments The arguments to the save method
     *
     * @return A {@link Single} with the result of the operation
     */
    Observable<D> save(Map<String, Object> arguments)

    /**
     * Deletes an entity
     *
     * @return An observable that returns a boolean true if successful
     */
    Observable<Boolean> delete()
}