package grails.gorm.tests

import grails.gorm.dirty.checking.DirtyCheck
import grails.neo4j.Neo4jEntity
import grails.persistence.Entity
import org.grails.datastore.gorm.Setup
import org.grails.datastore.gorm.neo4j.GraphPersistentEntity
import org.grails.datastore.gorm.neo4j.util.IteratorUtil
import org.grails.datastore.mapping.core.Session
import org.neo4j.graphdb.DynamicLabel
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Transaction
import spock.lang.Issue

/**
 * test for various strategies to define Neo4j labels on domain classes and instances
 * @author Stefan Armbruster
 */
class LabelStrategySpec extends GormDatastoreSpec {

    @Override
    List getDomainClasses() {
        [FinalSubClass, ParentClass, ClassInTheMiddle, SubClass, Default, StaticLabel, StaticLabels, DynLabel, MixedLabels, InstanceDependentLabels]
    }

    Transaction tx
    def setup() {
        def graph = serverControls.graph()
        tx = graph.beginTx()
    }

    def cleanup() {
        tx?.close()
    }



    def "test dynamic associations with dynamic labels"() {
        when:
        def s = new DynLabel(name:'dummy')
        s.test = new Default(name:'dummy')
        s.save(flush:true)

        then:
        verifyLabelsForId(s.id, [s.class.name])

        when:
        def d = DynLabel.findByName("dummy")

        then:
        d!=null
        d.id == s.id
        d.test != null

    }

    def "should default label mapping use simple class name"() {
        when:
        def d = new Default(name: 'dummy').save(flush: true)
        def labelName = d.class.simpleName

        then:
        verifyLabelsForId(d.id, [labelName])


        and:
        def graph = serverControls.graph()
        graph.schema().getIndexes(Label.label(labelName))*.propertyKeys == [['__id__']]
    }

    def "should static label mapping work"() {
        when:
        def s = new StaticLabel(name:'dummy').save(flush:true)
        def labelName = "MyLabel"

        then:
        verifyLabelsForId(s.id, [labelName])

        and:
        serverControls.graph().schema().getIndexes(Label.label(labelName))*.propertyKeys == [['__id__']]
    }

    def "should static label mapping work for multiple labels"() {
        when:
        def s = new StaticLabels(name:'dummy').save(flush:true)
        def labels = ["MyLabel1", "MyLabel2"]

        then:
        verifyLabelsForId(s.id, labels)

        and:
        labels.every {
            serverControls.graph().schema().getIndexes(Label.label(it))*.propertyKeys == [['__id__']]
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

        setup:
        setupValidator(InstanceDependentLabels)

        when:
        def s = new InstanceDependentLabels(name:'Sam', profession: 'Fireman')
        s.save(flush:true)

        then:
        s.hasErrors() == false
        verifyLabelsForId(s.id, ["InstanceDependentLabels", "${s.profession}"])

        and:
        serverControls.graph().schema().getIndexes(Label.label("MyLabel"))*.propertyKeys == [['__id__']]

        and: "no index on instance label"
        !serverControls.graph().schema().getIndexes(Label.label(s.name)).iterator().hasNext()

        and: "there are no indexes on null"
        serverControls.graph().schema().getIndexes().every { it.label.name() != 'null'}

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

    @Issue('https://jira.grails.org/browse/GPNEO4J-18')
    void 'test abstract classes do not contribute to labels'() {
        when:
        def parentInstance = new ParentClass(name: 'parent name').save(flush: true)
        def subclassInstance = new SubClass(subName: 'sub name', middleName: 'middle name', name: 'parent name').save(flush: true)
        def finalSubclassInstance = new FinalSubClass(finalName: 'final name', subName: 'sub name', middleName: 'middle name', name: 'parent name').save(flush: true)

        then:
        verifyLabelsForId(parentInstance.id, ['ParentClass'])
        verifyLabelsForId(subclassInstance.id, ['SubClass'])
        verifyLabelsForId(finalSubclassInstance.id, ['FinalSubClass', 'SubClass'])
    }

    private def verifyLabelsForId(id, labelz) {
        def cypherResult = session.transaction.nativeTransaction.run("MATCH (n {__id__:{1}}) return labels(n) as labels", ["1":id])

        def result = IteratorUtil.single(cypherResult)
        def labelsObject = result["labels"].asList()
        assert labelsObject as Set == labelz as Set
        true
    }
}

@DirtyCheck
@Entity
class ParentClass implements Neo4jEntity<ParentClass>{
    Long id
    Long version
    String name
}

@DirtyCheck
@Entity
abstract class ClassInTheMiddle extends ParentClass implements Neo4jEntity<ClassInTheMiddle>{
    String middleName
}

@DirtyCheck
@Entity
class SubClass extends ClassInTheMiddle implements Neo4jEntity<SubClass>{
    String subName
}

@DirtyCheck
@Entity
final class FinalSubClass extends SubClass implements Neo4jEntity<FinalSubClass> {
    String finalName
}

@DirtyCheck
@Entity
class Default implements Neo4jEntity<Default>{
    Long id
    Long version
    String name
}

@DirtyCheck
@Entity
class StaticLabels implements Neo4jEntity<StaticLabels>{
    Long id
    Long version
    String name

    static mapping = {
        labels "MyLabel1", "MyLabel2"
    }

}

@DirtyCheck
@Entity
class StaticLabel implements Neo4jEntity<StaticLabel>{
    Long id
    Long version
    String name

    static mapping = {
        labels "MyLabel"
    }
}

@DirtyCheck
@Entity
class DynLabel implements Neo4jEntity<DynLabel>{
    Long id
    Long version
    String name

    static mapping = {
        labels { GraphPersistentEntity pe -> "`${pe.javaClass.name}`" }
    }
}

@DirtyCheck
@Entity
class MixedLabels implements Neo4jEntity<MixedLabels>{
    Long id
    Long version
    String name

    static mapping = {
        labels "MixedLabel", { GraphPersistentEntity pe -> "`${pe.javaClass.name}`" }
    }
}

@DirtyCheck
@Entity
class InstanceDependentLabels implements Neo4jEntity<InstanceDependentLabels>{
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