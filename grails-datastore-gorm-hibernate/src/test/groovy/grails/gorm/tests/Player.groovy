package grails.gorm.tests

import grails.persistence.Entity

/**
 * Created by graemerocher on 01/02/16.
 */
@Entity
class Player {
    String name
    static belongsTo = [team:Team]
    static hasOne = [contract:Contract]
}
