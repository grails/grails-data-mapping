import grails.gorm.tests.GormDatastoreSpec

/**
 * @author graemerocher
 */
class BidirectionalOneToManyWithInheritanceSpec extends GormDatastoreSpec {

    static {
        GormDatastoreSpec.TEST_CLASSES << ConfigurationItem << Documentation <<  ChangeRequest
    }

    void "Test a bidirectional one-to-many association with inheritance"() {

        given:
            def doc = new Documentation()

            doc.addToConfigurationItems(new ChangeRequest())
              .addToConfigurationItems(new Documentation())

        when:
            doc.save(flush:true)
            session.clear()
            doc = Documentation.get(1)

        then:
            2 == doc.configurationItems.size()
    }
}

class ConfigurationItem {
    Long id
    Long version
    ConfigurationItem parent

    Set configurationItems

    static hasMany = [configurationItems: ConfigurationItem]
    static mappedBy = [configurationItems: 'parent']
    static belongsTo = [ConfigurationItem]
    static constraints = {
        parent(nullable: true)
    }
}

class Documentation extends ConfigurationItem {
    Long id
    Long version
}

class ChangeRequest extends ConfigurationItem {
    Long id
    Long version
}
