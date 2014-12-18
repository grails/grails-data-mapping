package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import static junit.framework.Assert.*
import org.junit.Test
/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 21, 2009
 */
class UnidirectionalOneToManyWithJoinTableTests extends AbstractGrailsHibernateTests {

    @Test
    void testUnidirectionalOneToManyWithExplicityJoinTable() {
        // will throw an exception if join table incorrectly mapped
        session.connection().prepareStatement("SELECT PROJECT_ID, EMPLOYEE_ID FROM EMP_PROJ").executeQuery()
    }

    @Override
    protected getDomainClasses() {
        [Project, Employee]
    }
}

@Entity
class Employee {
    Long id
    Long version
    Set projects
    static hasMany = [projects: Project]
    static mapping = { projects joinTable: [name: 'EMP_PROJ', column: 'PROJECT_ID', key: 'EMPLOYEE_ID'] }
}

@Entity
class Project {
    Long id
    Long version

    static belongsTo = Employee
}


