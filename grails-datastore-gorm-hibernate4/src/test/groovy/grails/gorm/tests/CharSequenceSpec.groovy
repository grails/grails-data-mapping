package grails.gorm.tests

import grails.persistence.Entity

/**
 * Created by graemerocher on 14/04/14.
 */
class CharSequenceSpec extends GormDatastoreSpec{


    void "Test GORM GString handling"() {

            given:"A domain instance"
                new SomeDomainClass(name:"hello").save(flush:true)
                session.clear()
                def value = 'hello'
                def queryArg = "${value}"


            expect:"Gstring handling to work with queries"
                queryArg instanceof GString
                SomeDomainClass.findByName(queryArg)
                SomeDomainClass.findByNameLike(queryArg)
                SomeDomainClass.countByName(queryArg)
                SomeDomainClass.countByNameLike(queryArg)
                SomeDomainClass.findAllByName(queryArg)
                SomeDomainClass.findAllByNameLike(queryArg)
                SomeDomainClass.findWhere(name:queryArg)
                SomeDomainClass.findAllWhere(name:queryArg)
                SomeDomainClass.withCriteria{ eq 'name',queryArg }
                SomeDomainClass.find("from SomeDomainClass s where s.name = ?", [queryArg])
                SomeDomainClass.findAll("from SomeDomainClass s where s.name = ?", [queryArg])
                SomeDomainClass.executeQuery("from SomeDomainClass s where s.name = ?", [queryArg])

    }

    @Override
    List getDomainClasses() {
        [Task, SomeDomainClass]
    }
}

@Entity
class SomeDomainClass {
    Long id
    Long version
    String name
}