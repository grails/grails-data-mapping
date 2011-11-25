package grails.gorm.tests

class OneToManySpec extends GormDatastoreSpec {
    @Override
    List getDomainClasses() {
        [Person, Country]
    }


    void "test save and return unidirectional one to many"() {
        given:
            Person p = new Person(firstName: "Fred", lastName: "Flinstone")
            Country c = new Country(name:"Dinoville")
                    .addToResidents(p)
                    .save(flush:true)

            session.clear()

        when:
            c = Country.findByName("Dinoville")

        then:

            c != null
            c.residents != null
            c.residents.size() == 1

        when:
            c.addToResidents(new Person(firstName:"Barney", lastName:"Rubble"))
            c.save(flush:true)
            session.clear()
            c = Country.findByName("Dinoville")

        then:
            c != null
            c.residents != null
            c.residents.size() == 2
    }
}
