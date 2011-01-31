import grails.gorm.tests.Pet

@grails.persistence.Entity
class Person implements Serializable{
  String id
  String firstName
  String lastName
  Set pets = [] as Set
  static hasMany = [pets:Pet]

  static mapping = {
    firstName index:true
    lastName index:true
  }
}