package grails.gorm.tests


import org.grails.datastore.mapping.proxy.ProxyHandler

class ProxyInitializationSpec extends GormDatastoreSpec {

    @Override
    List getDomainClasses() {
        [Patient, ContactDetails]
    }

    void "test if proxy is initialized"() {

        setup:
        final ProxyHandler proxyHandler = session.mappingContext.getProxyHandler()
        Long patientId = new Patient(contactDetails: new ContactDetails(phoneNumber: "+1-202-555-0105")).save(failOnError: true).id
        session.flush()
        session.clear()

        when:
        Patient patient = Patient.get(patientId)

        then:
        proxyHandler.isProxy(patient.contactDetails)

        when:
        patient.contactDetails.phoneNumber = "+1-202-555-0105"

        then:
        proxyHandler.isInitialized(patient.contactDetails)
    }
}
