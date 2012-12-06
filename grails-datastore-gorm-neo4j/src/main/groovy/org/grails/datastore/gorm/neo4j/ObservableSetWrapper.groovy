package org.grails.datastore.gorm.neo4j

import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.collection.PersistentSet
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 18.09.11
 * Time: 14:44
 * To change this template use File | Settings | File Templates.
 */
class ObservableSetWrapper implements Set, Externalizable {

    @Delegate PersistentSet wrapped
    def entity
    PropertyChangeListener propertyChangeListener
    Class clazz

    ObservableSetWrapper(def entity, Collection keys, Class clazz, Session session) {
        this.entity = entity
        this.propertyChangeListener = session
        this.clazz = clazz
        wrapped = new PersistentSet(keys, clazz, session)
    }

    ObservableSetWrapper() {
    }

    private void fireChangeEvent() {
        propertyChangeListener.propertyChange(new PropertyChangeEvent(entity, null, null, null))
    }

    boolean add(obj) {
        fireChangeEvent()
        wrapped.add(obj)
    }
    boolean remove(def obj) {
        fireChangeEvent()
        wrapped.remove(obj)
    }

    boolean addAll(Collection coll) {
        fireChangeEvent()
        wrapped.addAll(coll)
    }

    boolean retainAll(Collection coll) {
        fireChangeEvent()
        wrapped.retainAll(coll)
    }

    boolean removeAll(Collection coll) {
        fireChangeEvent()
        wrapped.removeAll(coll)
    }

    void clear() {
        fireChangeEvent()
        wrapped.clear()
    }

    void writeExternal(java.io.ObjectOutput objectOutput) throws java.io.IOException {
        objectOutput.writeObject()
        def collection = new HashSet(wrapped)
        objectOutput.writeObject(collection)
    }

    void readExternal(java.io.ObjectInput objectInput) throws java.io.IOException, java.lang.ClassNotFoundException {
        clazz = objectInput.readObject()
        Set collection = objectInput.readObject()
        wrapped = new PersistentSet(clazz, null, collection)

    }

}
