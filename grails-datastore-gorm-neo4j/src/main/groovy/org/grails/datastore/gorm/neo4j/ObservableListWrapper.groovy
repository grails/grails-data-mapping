package org.grails.datastore.gorm.neo4j

import java.beans.PropertyChangeEvent
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.collection.PersistentList

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 18.09.11
 * Time: 14:44
 * To change this template use File | Settings | File Templates.
 */
class ObservableListWrapper implements Serializable {

    @Delegate List wrapped
    def entity
    Collection keys
    Neo4jSession session

    ObservableListWrapper(def entity, Collection keys, Class clazz, Session session) {
        this.entity = entity
        this.session = session
        this.keys = keys
        wrapped = new PersistentList(keys, clazz, session)
    }

    boolean add(obj) {
        session.propertyChange(new PropertyChangeEvent(entity, null, null, null))
        wrapped.add(obj)
    }

    boolean remove(def obj) {
        session.propertyChange(new PropertyChangeEvent(entity, null, null, null))
        wrapped.remove(obj)
    }

    boolean addAll(Collection coll) {
        session.propertyChange(new PropertyChangeEvent(entity, null, null, null))
        wrapped.addAll(coll)
    }

    boolean retainAll(Collection coll) {
        session.propertyChange(new PropertyChangeEvent(entity, null, null, null))
        wrapped.retainAll(coll)
    }

    boolean removeAll(Collection coll) {
        session.propertyChange(new PropertyChangeEvent(entity, null, null, null))
        wrapped.removeAll(coll)
    }

    void clear() {
        session.propertyChange(new PropertyChangeEvent(entity, null, null, null))
        wrapped.clear()
    }

    int size() {
        keys.size()
    }

    public Object set(int index, Object element) {
        session.propertyChange(new PropertyChangeEvent(entity, null, null, null))
        wrapped.set(index, element)
    }

    public void add(int index, Object element) {
        session.propertyChange(new PropertyChangeEvent(entity, null, null, null))
        wrapped.add(index, element)
    }

    public Object remove(int index) {
        session.propertyChange(new PropertyChangeEvent(entity, null, null, null))
        wrapped.remove(index)
    }

    public boolean addAll(int index, Collection c) {
        session.propertyChange(new PropertyChangeEvent(entity, null, null, null))
        wrapped.addAll(index, c)
    }

}
