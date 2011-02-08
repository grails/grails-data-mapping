package grails.gorm.tests

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 23, 2010
 * Time: 1:43:52 PM
 * To change this template use File | Settings | File Templates.
 */
@grails.persistence.Entity
class TestEntity implements Serializable{
  Long id
  Long version
  String name
  Integer age = 30

  ChildEntity child

  static mapping = {
    name index:true
    age index:true, nullable:true
    child index:true, nullable:true
  }
  
  static constraints = {
	  name blank:false
	  child nullable:true
  }
}
