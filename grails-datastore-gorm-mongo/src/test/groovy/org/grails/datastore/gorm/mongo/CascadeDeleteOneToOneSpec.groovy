package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import org.bson.types.ObjectId
import grails.persistence.Entity

/**
 * @author Graeme Rocher
 */
class CascadeDeleteOneToOneSpec extends GormDatastoreSpec{

    void "Test owner deletes child in one-to-one cascade"() {

       when:"A owner with a one-to-one relation is persisted"
           def u = new SystemUser(name:"user2")
           u.settings = new UserSettings(user:u)
           u.save(flush:true)
           session.clear()
           def found1 = SystemUser.findByName("user2")
           def found1a = UserSettings.findByUser(found1)

        then:"The user is found"
            found1 != null
            found1a != null

        when:"An owner is deleted"
           found1.delete(flush:true)
           def found2 = SystemUser.findByName("user2")
           def found1b = UserSettings.findByUser(found1)

        then:"The child association is deleted too"
            assert found2 == null
            assert found1b == null
    }

    @Override
    List getDomainClasses() {
        [SystemUser, UserSettings]
    }

}

@Entity
class SystemUser {
   ObjectId id

   String name
   UserSettings settings
}

@Entity
class UserSettings {
   ObjectId id

   boolean someSetting = true

   SystemUser user
   static belongsTo = [user:SystemUser]

   static mapping = {
      collection "user_settings"
   }
}
