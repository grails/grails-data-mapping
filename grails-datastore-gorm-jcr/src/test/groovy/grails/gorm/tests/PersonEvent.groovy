@grails.persistence.Entity
class PersonEvent implements Serializable{
    String id
    Long version
    String name
    Date dateCreated
    Date lastUpdated

    static STORE = [updated:0, inserted:0]

    static void resetStore() { STORE = [updated:0, inserted:0] }

    def beforeDelete() {
      STORE["deleted"] = true

    }
    def beforeUpdate() {
      STORE["updated"]++
    }
    def beforeInsert() {
      STORE["inserted"]++
    }

}