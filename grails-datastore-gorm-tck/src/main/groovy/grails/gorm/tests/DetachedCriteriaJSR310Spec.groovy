package grails.gorm.tests

import grails.gorm.DetachedCriteria
import grails.persistence.Entity
import org.springframework.util.ClassUtils
import spock.lang.IgnoreIf
import spock.lang.Issue
import spock.lang.Specification

import java.time.LocalDateTime

@IgnoreIf( { !ClassUtils.isPresent( "java.time.LocalDateTime", getClass().getClassLoader()) })
class DetachedCriteriaJSR310Spec extends Specification {

    @Issue('https://github.com/grails/grails-data-mapping/issues/1003')
    void "test updateAll works with jsr310 dates"() {
        given:
        new NewDateTypes(name: "John", age: 55).save()
        new NewDateTypes(name: "Sally", age: 21).save()
        new NewDateTypes(name: "Susan", age: 22).save()
        new NewDateTypes(name: "Joe", age: 45).save(flush: true)

        when:
        def criteria = new DetachedCriteria(NewDateTypes).build { }
        int total = criteria.updateAll(age: 19)
        List<NewDateTypes> results = NewDateTypes.all

        then:
        total == 4
        noExceptionThrown()
        results[0].dateCreated < results[0].lastUpdated
        results[1].dateCreated < results[1].lastUpdated
        results[2].dateCreated < results[2].lastUpdated
        results[3].dateCreated < results[3].lastUpdated
    }

}


@Entity
class NewDateTypes {
    String name
    Integer age

    LocalDateTime dateCreated
    LocalDateTime lastUpdated
}