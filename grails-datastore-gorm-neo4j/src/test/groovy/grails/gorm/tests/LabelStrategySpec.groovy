package grails.gorm.tests

import grails.persistence.Entity
import org.grails.datastore.gorm.Setup
import org.neo4j.graphdb.DynamicLabel
import org.neo4j.helpers.collection.IteratorUtil

/**
 * test for various strategies to define Neo4j labels on domain classes and instances
 * @author Stefan Armbruster
 */
class LabelStrategySpec extends GormDatastoreSpec {

    @Override
        List getDomainClasses() {
            [Default, StaticLabel, StaticLabels]
        }


    def "should default label mapping use simple class name"() {
        when:
        def d = new Default(name:'dummy').save(flush:true)

        then:
        verifyLabelsForId(d.id, [d.class.simpleName])
    }

    def "should static label mapping work"() {
        when:
        def s = new StaticLabel(name:'dummy').save(flush:true)

        then:
        verifyLabelsForId(s.id, ["MyLabel"])

//        and:
        // IteratorUtil.count(Setup.graphDb.schema().getConstraints(DynamicLabel.label("MyLabel"))) == 1 // test run without indexes
    }

    def "should static label mapping work for multiple labels"() {
        when:
        def s = new StaticLabels(name:'dummy').save(flush:true)

        then:
        verifyLabelsForId(s.id, ["MyLabel1", "MyLabel2"])

//        and:
        // IteratorUtil.count(Setup.graphDb.schema().getConstraints(DynamicLabel.label("MyLabel"))) == 1 // test run without indexes
    }

    private def verifyLabelsForId(id, labelz) {
        def cypherResult = session.nativeInterface.execute("MATCH (n {__id__:{1}}) return labels(n) as labels", [id])
        assert IteratorUtil.first(cypherResult)["labels"] == labelz
        true
    }
}

@Entity
class Default {
    Long id
    Long version
    String name
}

@Entity
class StaticLabel {
    Long id
    Long version
    String name

    static mapping = {
        labels "MyLabel"
    }
}

@Entity
class StaticLabels {
    Long id
    Long version
    String name

    static mapping = {
        labels "MyLabel1", "MyLabel2"
    }

}

