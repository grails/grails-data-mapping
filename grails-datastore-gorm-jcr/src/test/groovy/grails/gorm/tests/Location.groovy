@grails.persistence.Entity
class Location implements Serializable {
    String id
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
