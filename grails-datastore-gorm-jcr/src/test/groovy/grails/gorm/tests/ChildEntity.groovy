package grails.gorm.tests

/**
 * Override from GORM TCK to test String based Id
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
@grails.persistence.Entity
class ChildEntity implements Serializable{
  String id;
  String name

  static mapping = {
    name index:true
  }
  static belongsTo = [TestEntity]
}