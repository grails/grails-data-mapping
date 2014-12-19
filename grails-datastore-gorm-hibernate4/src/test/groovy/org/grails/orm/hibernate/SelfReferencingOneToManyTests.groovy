package org.grails.orm.hibernate

import grails.persistence.Entity

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class SelfReferencingOneToManyTests extends AbstractGrailsHibernateTests {

    @Override
    protected getDomainClasses() {
        [SelfReferencingOneToManyCategory]
    }

    @Test
    void testCascadingDeletes() {
        assertNotNull SelfReferencingOneToManyCategory.newInstance(name:"Root")
                                   .addToChildren(name:"Child 1")
                                   .addToChildren(name:"Child 2")
                                   .save(flush:true)

        session.clear()

        def category = SelfReferencingOneToManyCategory.get(1)
        def child = category.children.find { it.name == 'Child 1' }

        category.removeFromChildren(child)
        child.delete(flush:true)

        session.clear()

        category = SelfReferencingOneToManyCategory.get(1)
        assertNotNull category
        assertEquals 1, category.children.size()
    }

    @Test
    void testThreeLevelCascadingDeleteToChildren() {

        def root = SelfReferencingOneToManyCategory.newInstance(name:"Root")

        def child1 = SelfReferencingOneToManyCategory.newInstance(name:"Child 1")
                                  .addToChildren(name:"Second Level Child 1")
                                  .addToChildren(name:"Second Level Child 2")

        def child2 = SelfReferencingOneToManyCategory.newInstance(name:"Child 2")
                                  .addToChildren(name:"Second Level Child 1")
                                  .addToChildren(name:"Second Level Child 2")

        root.addToChildren(child1)
        root.addToChildren(child2)

        root.save(flush:true)

        session.clear()

        def category = SelfReferencingOneToManyCategory.get(1)
        def child = category.children.find { it.name == 'Child 1' }

        assertEquals 2, child.children.size()
        assertEquals category, child.parent

        category.removeFromChildren(child)
        child.delete(flush:true)

        session.clear()

        category = SelfReferencingOneToManyCategory.get(1)
        assertNotNull category
        assertEquals 1, category.children.size()
    }
}

@Entity
class SelfReferencingOneToManyCategory {
    Long id
    Long version

    String name

    Set children
    SelfReferencingOneToManyCategory parent
    static hasMany = [children: SelfReferencingOneToManyCategory]
    static belongsTo = [parent: SelfReferencingOneToManyCategory]

    static constraints = {
        parent(nullable: true)
    }
}
