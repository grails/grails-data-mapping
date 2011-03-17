@grails.persistence.Entity
class ModifyPerson implements Serializable {
    String id
    Long version

    String name

    def beforeInsert() {
        name = "Fred"
    }
}
