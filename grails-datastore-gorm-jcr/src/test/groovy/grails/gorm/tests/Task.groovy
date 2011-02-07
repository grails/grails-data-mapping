@grails.persistence.Entity
class Task implements Serializable{
    String id
    Set tasks
    Task task
    String name

    static mapping = {
      name index:true
    }
    static hasMany = [tasks:Task]
}