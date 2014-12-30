package org.grails.orm.hibernate

import grails.persistence.Entity;

import org.junit.Test

import static junit.framework.Assert.*
/**
 * @author Graeme Rocher
 * @since 1.0
 */
class EnumMappingTests extends AbstractGrailsHibernateTests {


    @Test
    void testEnumNullabilityWithTablePerHierarchy() {
        def domainObject = SubClassWithStringPropertyEnumMapping.newInstance()
        domainObject.someProperty = 'data'
        assertNotNull domainObject.save()

        domainObject = SubClassWithEnumPropertyEnumMapping.newInstance()
        assertNull domainObject.save()
        domainObject.vehicalStatus = VehicleStatus.IDLING
        assertNotNull domainObject.save()

        domainObject = SubClassWithOptionalEnumPropertyEnumMapping.newInstance()
        assertNotNull domainObject.save()
    }

    @Test
    void testDefaultEnumMapping() {
        def vehicle = Vehicle.newInstance()

        vehicle.status = VehicleStatus.IDLING
        vehicle.save(flush:true)

        session.clear()

        vehicle = Vehicle.get(1)
        assertEquals VehicleStatus.IDLING, vehicle.status

        def con = session.connection()
        def ps = con.prepareStatement("select * from vehicle")
        def rs = ps.executeQuery()
        rs.next()
        assertEquals VehicleStatus.IDLING.toString(),rs.getString("status")

        con.close()
    }

    @Test
    void testOrdinalEnumMapping() {
        def vehicle = Vehicle2.newInstance()

        vehicle.status = VehicleStatus.IDLING
        vehicle.save(flush:true)

        session.clear()

        vehicle = Vehicle2.get(1)
        assertEquals VehicleStatus.IDLING, vehicle.status

        def con = session.connection()
        def ps = con.prepareStatement("select * from vehicle2")
        def rs = ps.executeQuery()
        rs.next()
        assertEquals 1,rs.getInt("status")

        con.close()
    }

    @Test
    void testCustomTypeEnumMapping() {
        def instance = TestEnumUser.newInstance()

        instance.usesCustom = TestEnum.Flurb
        instance.doesnt = TestEnum.Skrabdle
        instance.save(flush:true)
        session.clear()

        instance = TestEnumUser.get(instance.id)
        assertEquals TestEnum.Flurb, instance.usesCustom
        assertEquals TestEnum.Skrabdle, instance.doesnt

        def con = session.connection()
        def ps = con.prepareStatement('select * from test_enum_user')
        def rs = ps.executeQuery()
        rs.next()

        assertEquals 4200, rs.getInt('uses_custom')
        assertEquals 'Skrabdle', rs.getString('doesnt')

        rs.close()
        ps.close()
        con.close()
    }

    @Override
    protected getDomainClasses() {
        [Vehicle, Vehicle2, EnumMappingSuperClass, SubClassWithEnumPropertyEnumMapping, SubClassWithOptionalEnumPropertyEnumMapping, SubClassWithStringPropertyEnumMapping, TestEnumUser]
    }
}
enum VehicleStatus { OFF, IDLING, ACCELERATING, DECELARATING }

@Entity
class Vehicle {
    Long id
    Long version
    VehicleStatus status
}
@Entity
class Vehicle2 {
    Long id
    Long version
    VehicleStatus status
    static mapping = {
        status enumType:'ordinal'
    }
}
@Entity
class EnumMappingSuperClass {Long id; Long version;}

@Entity
class SubClassWithStringPropertyEnumMapping extends EnumMappingSuperClass {
    Long id
    Long version
    String someProperty
}
@Entity
class SubClassWithEnumPropertyEnumMapping extends EnumMappingSuperClass {
    VehicleStatus vehicalStatus
}
@Entity
class SubClassWithOptionalEnumPropertyEnumMapping extends EnumMappingSuperClass {
    VehicleStatus optionalVehicalStatus
    static constraints = {
        optionalVehicalStatus nullable: true
    }
}

@Entity
class TestEnumUser {
    Long id
    Long version
    TestEnum usesCustom
    TestEnum doesnt
    static mapping = {
        usesCustom type: TestEnumUserType
    }
}
