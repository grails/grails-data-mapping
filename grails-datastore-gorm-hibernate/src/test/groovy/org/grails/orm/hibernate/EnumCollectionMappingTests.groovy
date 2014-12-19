package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*
/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 21, 2009
 */
class EnumCollectionMappingTests extends AbstractGrailsHibernateTests {


    @Test
    void testCollectionOfEnumMapping() {

        def conn = session.connection()
        conn.prepareStatement("SELECT TRUCK_ID, THE_STATUS FROM VEHICLE_STATUS_LOG").executeQuery()

        def truck = EnumCollectionTruck.newInstance()
        truck.addToStatuses(EnumCollectionVehicleStatus.OFF)
        truck.addToStatuses(EnumCollectionVehicleStatus.IDLING)
        truck.save(flush:true)

        session.clear()

        truck = EnumCollectionTruck.get(1)
        assertNotNull truck
        assertEquals 2, truck.statuses.size()
        assertTrue truck.statuses.contains(EnumCollectionVehicleStatus.OFF)
        assertTrue truck.statuses.contains(EnumCollectionVehicleStatus.IDLING)
    }

    @Test
    void testDefaultCollectionOfEnumMapping() {

        def conn = session.connection()
        conn.prepareStatement("SELECT enum_collection_truck_id, enum_collection_vehicle_status FROM enum_collection_truck_more_statuses").executeQuery()

        def truck = EnumCollectionTruck.newInstance()

        truck.addToMoreStatuses(EnumCollectionVehicleStatus.OFF)
        truck.addToMoreStatuses(EnumCollectionVehicleStatus.IDLING)
        truck.save(flush:true)

        session.clear()

        truck = EnumCollectionTruck.get(1)
        assertNotNull truck
        assertEquals 2, truck.moreStatuses.size()
        assertTrue truck.moreStatuses.contains(EnumCollectionVehicleStatus.OFF)
        assertTrue truck.moreStatuses.contains(EnumCollectionVehicleStatus.IDLING)
    }

    @Override
    protected getDomainClasses() {
        [EnumCollectionTruck, EnumCollectionVehicleStatus]
    }
}


enum EnumCollectionVehicleStatus { OFF, IDLING, ACCELERATING, DECELARATING }

@Entity
class EnumCollectionTruck {
    Long id
    Long version

    Set statuses, moreStatuses
    static hasMany = [statuses:EnumCollectionVehicleStatus,moreStatuses:EnumCollectionVehicleStatus]

    static mapping = {
        statuses joinTable:[name:'VEHICLE_STATUS_LOG', key:'TRUCK_ID', column:'THE_STATUS']
    }
}
