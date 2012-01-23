package org.grails.datastore.gorm

import javax.persistence.Entity
import grails.gorm.tests.GormDatastoreSpec

/**
 *
 */
class NestedAssociationQuerySpec extends GormDatastoreSpec{


    void "Test that a join query can be applied to a nested association"() {
        given:"A domain multipe with nested asssociations"
            def qText = 'What is your gender?'
            def genderQuestion = createQuestion(qText)
            def femaleAnswer   = createAnswer('Female', genderQuestion)
            def userOpinion    = createUserOpinion(femaleAnswer)

        when:"The association is queried"
            def c = UserOpinion.createCriteria()
            def femaleCnt = c.count {
                answers {
                    and {
                        question {
                            eq('text', qText)
                        }
                        eq('text', 'Female')
                    }
                }
            }

        then:"The right result is returned"
            femaleCnt == 1
    }

    private Question createQuestion(String qText) {
        new Question(text: qText).save(flush: true)
    }

    private Answer createAnswer(String aText, Question q) {
        new Answer(text: aText, question: q).save(flush: true)
    }

    private UserOpinion createUserOpinion(Answer answer) {
        def userOpinion = new UserOpinion()
        userOpinion.answers << answer

        userOpinion.save(flush: true)
    }

    @Override
    List getDomainClasses() {
        [UserOpinion, Answer, Question]
    }
}

@Entity
class UserOpinion {

    Long id
    Set answers = []
    static hasMany = [answers: Answer]

    static constraints = {
    }
}

@Entity
class Answer {

    Long id
    String text
    Question question

    static constraints = {
        text(blank: false)
    }
}

@Entity
class Question {

    Long id
    String text

    static constraints = {
        text(blank: false)
    }
}
