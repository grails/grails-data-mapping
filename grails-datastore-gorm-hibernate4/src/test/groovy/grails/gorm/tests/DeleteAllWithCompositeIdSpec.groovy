package grails.gorm.tests

import grails.persistence.Entity
import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform
import org.grails.orm.hibernate.GormSpec

/**
 * Created by graemerocher on 17/12/15.
 */
@ApplyDetachedCriteriaTransform
class DeleteAllWithCompositeIdSpec extends GormSpec {


    void "Test deleteAll with where query"() {

        given:
        def u1 = new User(name: 'admin', createTime: new Date()).save(failOnError: true)
        def r1 = new Role(name: 'ROLE_ADMIN').save(failOnError: true)
        new UserRole(user: u1, role: r1).save(failOnError: true)
        session.clear()
        when:
        def u = User.first()
        def r = Role.first()

        UserRole.where { user == u && role == r }.deleteAll()

        then:
        UserRole.count() == 0
    }
    @Override
    List getDomainClasses() {
        [User, Role, UserRole]
    }
}

@Entity
class Role {
    String name
}
@Entity
class UserRole implements Serializable {

    User user
    Role role

    static mapping = {
        id composite: ['user', 'role']
        version false
    }
}