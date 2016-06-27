package grails.gorm.tests

import grails.gorm.annotation.Entity
import org.grails.orm.hibernate.GormSpec

/**
 * Created by graemerocher on 27/06/16.
 */
class ManyToOneSpec extends GormSpec {

    void "Test many-to-one association"() {
        when:"A many-to-one association is saved"
        Foo foo1 = new Foo(fooDesc: "Foo One").save()
        Foo foo2 = new Foo(fooDesc: "Foo Two").save()

        foo1.bar = new Bar(barDesc: "Bar One",foo:foo1)
        foo1.save(flush:true)
        foo2.bar = new Bar(barDesc: "Bar Two", foo:foo2)
        foo2.save(flush:true)

        session.clear()

        foo1 = Foo.get(foo1.id)
        foo2 = Foo.get(foo2.id)


        Bar bar1 = Bar.findByBarDesc("Bar One")
        Bar bar2 = Bar.findByBarDesc("Bar Two")

        then:"The data model is correct"
        foo1.fooDesc == "Foo One"
        foo1.bar.barDesc == "Bar One"
        foo2.fooDesc == "Foo Two"
        foo2.bar.barDesc == "Bar Two"
        bar1.barDesc == "Bar One"
        bar1.foo.fooDesc == "Foo One"
        bar2.barDesc == "Bar Two"
        bar2.foo.fooDesc == "Foo Two"

        when:
        session.clear()
        def foos = Foo.findAll()
        println("Foos:")
        foos.each{ f ->
            println(f.fooDesc + " -> " + f.bar.barDesc)
        }

        def bars = Bar.findAll()
        println("Bars:")
        bars.each{ b ->
            println(b.barDesc + " -> " + b.foo.fooDesc)
        }

        then:
        foos*.bar
        bars*.foo
    }
    @Override
    List getDomainClasses() {
        [Foo,Bar]
    }
}

@Entity
class Foo {

    String fooDesc

    Bar bar

    static mapping = {
        id generator:'identity'
    }

    static constraints = {
        bar(nullable: true)
    }
}

@Entity
class Bar {

    String barDesc

    static belongsTo = [ foo: Foo ]

    static mapping = {
        id generator:'identity'
    }

    static constraints = {
    }
}