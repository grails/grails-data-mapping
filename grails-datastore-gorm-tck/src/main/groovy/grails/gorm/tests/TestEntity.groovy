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
  String name
  Integer age

  ChildEntity child

  static mapping = {
    name index:true
    age index:true
    child index:true
  }
}
