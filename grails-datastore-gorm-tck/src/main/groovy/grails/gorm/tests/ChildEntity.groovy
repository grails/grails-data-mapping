package grails.gorm.tests

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 23, 2010
 * Time: 1:43:52 PM
 * To change this template use File | Settings | File Templates.
 */
@grails.persistence.Entity
class ChildEntity implements Serializable{
  Long id
  Long version
  String name

  static mapping = {
    name index:true
  }
  static belongsTo = [TestEntity]
}