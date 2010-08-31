package org.grails.datastore.gorm

import org.springframework.validation.Validator
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import grails.gorm.tests.City
import grails.gorm.tests.Country
import grails.gorm.tests.Location
import grails.gorm.tests.CommonTypes
import grails.gorm.tests.ChildEntity
import grails.gorm.tests.TestEntity
import org.springframework.datastore.core.Session
import org.springframework.datastore.mock.SimpleMapDatastore
import org.springframework.datastore.mapping.PersistentEntity

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 30, 2010
 * Time: 5:06:49 PM
 * To change this template use File | Settings | File Templates.
 */
class Setup {
  static Session setup(classes) {
    def simple = new SimpleMapDatastore()
    for(cls in classes) {
      simple.mappingContext.addPersistentEntity(cls)
    }

    PersistentEntity entity = simple.mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("TestEntity")}


    simple.mappingContext.addEntityValidator(entity, [
            supports: { Class c -> true },
            validate: { Object o, Errors errors ->
                if(!StringUtils.hasText(o.name)) {
                  errors.rejectValue("name", "name.is.blank")
                }
            }
    ] as Validator)

    new GormEnhancer(simple).enhance()

    def con = simple.connect()
    return con
  }

}
