package org.grails.datastore.mapping.collection

import org.springframework.util.ReflectionUtils
import org.springframework.util.SerializationUtils
import spock.lang.Issue
import spock.lang.Specification

/**
 * Created by lari on 27/01/15.
 */
class PersistentSetSpec extends Specification {

    @Issue("GRAILS-11929")
    def "should support serialization after initialized"() {
        given:
        PersistentSet pset = new PersistentSet(String, null, ['a','b','c'] as Set)
        def initializedField = ReflectionUtils.findField(AbstractPersistentCollection, "initialized")
        ReflectionUtils.makeAccessible(initializedField)
        ReflectionUtils.setField(initializedField, pset, true)
        when:
        def psetSerialized = SerializationUtils.deserialize(SerializationUtils.serialize(pset))
        then:
        psetSerialized == ['a','b','c'] as Set
        psetSerialized == pset

    }

    def "will throw exception if non-initialized serialized collection is accessed"() {
        given:
        PersistentSet pset = new PersistentSet(String, null, ['a','b','c'] as Set)
        def psetSerialized = SerializationUtils.deserialize(SerializationUtils.serialize(pset))
        when:
        psetSerialized.iterator()
        then:
        thrown IllegalStateException
    }
}
