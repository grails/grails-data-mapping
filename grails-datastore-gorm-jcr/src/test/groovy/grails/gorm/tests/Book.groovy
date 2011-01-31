@grails.persistence.Entity
class Book implements Serializable{
    String id
    String author
    String title
    Boolean published

    static mapping = {
      published index:true
      title index:true
      author index:true
    }
}