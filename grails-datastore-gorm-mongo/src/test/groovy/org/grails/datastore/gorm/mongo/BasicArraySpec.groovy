package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity
import org.bson.types.ObjectId

/**
 * @author Graeme Rocher
 */
class BasicArraySpec extends GormDatastoreSpec{

    void "Test that arrays are saved correctly"() {
        when:"An entity with an array is saved"
            Data data = new Data(str: "foo", strArray: ["foo", "bar"] as String[]).save(flush:true)
            session.clear()
            data = Data.findByStr("foo")

        then:"The array is saved correct"
            data.str == "foo"
            data.strArray[0] == "foo"
            data.strArray[1] == 'bar'
    }

    @Override
    List getDomainClasses() {
        [Data]
    }
}


@Entity
class Data {

    ObjectId id

    String str

    String[] strArray


    @Override
    public String toString() {
        return "Data{" +
                "id=" + id +
                ", str='" + str + '\'' +
                ", strArray=" + (strArray == null ? null : Arrays.asList(strArray)) +
                '}';
    }
}