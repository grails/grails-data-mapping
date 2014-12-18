package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.transaction.support.TransactionSynchronizationManager
import static junit.framework.Assert.*
import org.junit.Test

/**
* @author Graeme Rocher
* @since 1.0
*
* Created: Mar 13, 2008
*/
class MergeDetachedObjectTests extends AbstractGrailsHibernateTests {


    @Test
    void testMergeDetachedObject() {

        def question = DetachedQuestion.newInstance(name:"What is the capital of France?")
                                    .addToAnswers(name:"London")
                                    .addToAnswers(name:"Paris")
                                    .save(flush:true)
        assertNotNull question

        session.clear()

        question = DetachedQuestion.get(1)

        session.clear()

        question = question.merge()
        assertEquals 2, question.answers.size()
        question.name = "changed"
        question.save(flush:true)
    }

    @Test
    void testStaticMergeMethod() {
        def question = DetachedQuestion.newInstance(name:"What is the capital of France?")
                                    .addToAnswers(name:"London")
                                    .addToAnswers(name:"Paris")
                                    .save(flush:true)
        assertNotNull question

        session.clear()

        question = DetachedQuestion.get(1)

        session.clear()

        question = DetachedQuestion.merge(question)
        assertEquals 2, question.answers.size()

        question.name = "changed"
        question.save(flush:true)
    }

    @Override
    protected getDomainClasses() {
        [DetachedQuestion, DetachedAnswer]
    }
}


@Entity
class DetachedQuestion {
    Long id
    Long version

    String name
    Set answers
    static hasMany = [answers:DetachedAnswer]
}

@Entity
class DetachedAnswer {
    Long id
    Long version

    String name
}
