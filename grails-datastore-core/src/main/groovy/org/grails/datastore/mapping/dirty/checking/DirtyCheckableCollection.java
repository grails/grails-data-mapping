package org.grails.datastore.mapping.dirty.checking;

/**
 * @author Graeme Rocher
 * @since 4.1
 */
public interface DirtyCheckableCollection {

    /**
     * @return True if the collection has changed
     */
    boolean hasChanged();
}
