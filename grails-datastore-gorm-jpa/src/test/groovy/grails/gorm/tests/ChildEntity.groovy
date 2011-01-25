package grails.gorm.tests

import grails.gorm.JpaEntity 
import java.io.Serializable;

@JpaEntity
class ChildEntity implements Serializable{
  String name

  static mapping = {
    name index:true
  }
  static belongsTo = [TestEntity]
}