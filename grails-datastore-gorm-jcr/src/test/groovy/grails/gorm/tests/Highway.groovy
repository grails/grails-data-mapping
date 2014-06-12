@grails.persistence.Entity
class Highway implements Serializable {
    String id
    Boolean bypassed
    String name

    static mapping = {
        bypassed index:true
        name index:true
    }
}
