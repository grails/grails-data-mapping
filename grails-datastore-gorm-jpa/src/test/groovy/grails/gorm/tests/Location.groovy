package grails.gorm.tests

import grails.gorm.JpaEntity 
import java.io.Serializable;


@JpaEntity
class Location implements Serializable{
  String name
  String code

  def namedAndCode() {
      "$name - $code"
  }
  
  static mapping = {
    name index:true
    code index:true
  }
}


