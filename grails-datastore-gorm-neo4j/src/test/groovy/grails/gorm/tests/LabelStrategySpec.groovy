package grails.gorm.tests

import grails.gorm.dirty.checking.DirtyCheck
import grails.persistence.Entity
import org.grails.datastore.gorm.Setup
import org.grails.datastore.gorm.neo4j.GraphPersistentEntity
import org.grails.datastore.mapping.core.Session
import org.neo4j.graphdb.DynamicLabel
import org.neo4j.helpers.collection.IteratorUtil
import spock.lang.Issue

/**
 * test for various strategies to define Neo4j labels on domain classes and instances
 * @author Stefan Armbruster
 */
class LabelStrategySpec extends GormDatastoreSpec {

    @Override
    List getDomainClasses() {
        [Default, StaticLabel, StaticLabels, DynLabel, MixedLabels, InstanceDependentLabels]
    }

    def setupSpec() {
        setupClass = LocalSetup
    }

    public class LocalSetup extends Setup {
        @Override
        static Session setup(classes) {
            skipIndexSetup = false
            extendedValidatorSetup = { mappingContext, grailsApplication ->
                setupValidator(mappingContext, grailsApplication, InstanceDependentLabels.simpleName)
            }
            Setup.setup(classes)
        }
    }

    def "should default label mapping use simple class name"() {
        when:
        def d = new Default(name: 'dummy').save(flush: true)
        def labelName = d.class.simpleName

        then:
        verifyLabelsForId(d.id, [labelName])

        and:
        Setup.graphDb.schema().getIndexes(DynamicLabel.label(labelName))*.propertyKeys == [['__id__']]
    }

    def "should static label mapping work"() {
        when:
        def s = new StaticLabel(name:'dummy').save(flush:true)
        def labelName = "MyLabel"

        then:
        verifyLabelsForId(s.id, [labelName])

        and:
        Setup.graphDb.schema().getIndexes(DynamicLabel.label(labelName))*.propertyKeys == [['__id__']]
    }

    def "should static label mapping work for multiple labels"() {
        when:
        def s = new StaticLabels(name:'dummy').save(flush:true)
        def labels = ["MyLabel1", "MyLabel2"]

        then:
        verifyLabelsForId(s.id, labels)

        and:
        labels.every {
            Setup.graphDb.schema().getIndexes(DynamicLabel.label(it))*.propertyKeys == [['__id__']]
        }

        when:
        def d = StaticLabels.findByName("dummy")

        then:
        d!=null
        d.id == s.id}

    def "should dynamic label mapping work"() {
        when:
        def s = new DynLabel(name:'dummy').save(flush:true)

        then:
        verifyLabelsForId(s.id, [s.class.name])

        when:
        def d = DynLabel.findByName("dummy")

        then:
        d!=null
        d.id == s.id
    }

    def "should mixed labels mapping work"() {
        when:
        def s = new MixedLabels(name:'dummy').save(flush:true)

        then:
        verifyLabelsForId(s.id, ["MixedLabel", s.class.name])

        when:
        def d = MixedLabels.findByName("dummy")

        then:
        d!=null
        d.id == s.id
    }

    @Issue("https://jira.grails.org/browse/GPNEO4J-17")
    def "should instance dependent labels mapping work"() {

        when:
        def s = new InstanceDependentLabels(name:'Sam', profession: 'Fireman')
        s.save(flush:true)

        then:
        s.hasErrors() == false
        verifyLabelsForId(s.id, ["InstanceDependentLabels", "${s.profession}"])

        and:
        Setup.graphDb.schema().getIndexes(DynamicLabel.label("MyLabel"))*.propertyKeys == [['__id__']]

        and: "no index on instance label"
        !Setup.graphDb.schema().getIndexes(DynamicLabel.label(s.name)).iterator().hasNext()

        and: "there are no indexes on null"
        Setup.graphDb.schema().getIndexes().every { it.label.name() != 'null'}

        when: "create Sam again, now as policeman"
        def d = new InstanceDependentLabels(name:'Sam', profession: 'Policeman')
        d.save(flush:true)

        then: "we've violated unique constraint"
        d.hasErrors() == true
        d.errors.allErrors[0].code == "unique"
        d.errors

        when: "unmarshall Sam instance"
        def sam = InstanceDependentLabels.findByName("Sam")

        then:
        sam != null
        sam.profession == "Fireman"
        s.id == sam.id

    }

    private def verifyLabelsForId(id, labelz) {
        def cypherResult = session.nativeInterface.execute("MATCH (n {__id__:{1}}) return labels(n) as labels", [id])
        assert IteratorUtil.first(cypherResult)["labels"] == labelz
        true
    }
}

@DirtyCheck
@Entity
class Default {
    Long id
    Long version
    String name
}

@DirtyCheck
@Entity
class StaticLabels {
    Long id
    Long version
    String name

    static mapping = {
        labels "MyLabel1", "MyLabel2"
    }

}

@DirtyCheck
@Entity
class StaticLabel {
    Long id
    Long version
    String name

    static mapping = {
        labels "MyLabel"
    }
}

@DirtyCheck
@Entity
class DynLabel {
    Long id
    Long version
    String name

    static mapping = {
        labels { GraphPersistentEntity pe -> "`${pe.javaClass.name}`" }
    }
}

@DirtyCheck
@Entity
class MixedLabels {
    Long id
    Long version
    String name

    static mapping = {
        labels "MixedLabel", { GraphPersistentEntity pe -> "`${pe.javaClass.name}`" }
    }
}

@DirtyCheck
@Entity
class InstanceDependentLabels {
    Long id
    Long version
    String name
    String profession

    static constraints = {
        name unique:true
    }
    static mapping = {
        labels { GraphPersistentEntity pe, instance ->  // 2 arguments: instance dependent label
            "`${instance.profession}`"
        }
    }
}