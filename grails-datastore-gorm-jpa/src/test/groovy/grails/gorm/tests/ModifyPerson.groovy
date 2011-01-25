package grails.gorm.tests

import grails.gorm.JpaEntity 
import java.io.Serializable;
 
@JpaEntity
class ModifyPerson implements Serializable{
  Long version

  String name

  void beforeInsert() {
    name = "Fred"
  }
}
