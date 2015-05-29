package grails.gorm.tests

import grails.persistence.Entity

/**
 * @author graemerocher
 */
@Entity
class ChildEntity implements Serializable {
    Long id
    Long version
    String name

    static transients = ['beforeValidateCounter']
    int beforeValidateCounter = 0

    def beforeValidate() {
        beforeValidateCounter++
    }

    static mapping = {
        name index:true
    }

    static belongsTo = [TestEntity]
}