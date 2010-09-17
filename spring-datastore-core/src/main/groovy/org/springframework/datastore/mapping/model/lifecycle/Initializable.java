package org.springframework.datastore.mapping.model.lifecycle;

/**
 * Interface for object that requires explicit initialization
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface Initializable {

    /**
     * Call to initialize the object
     */
    void initialize();
}
