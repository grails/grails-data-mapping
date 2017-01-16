package grails.gorm.tests

import grails.gorm.multitenancy.CurrentTenant
import grails.gorm.transactions.Transactional
import org.grails.datastore.mapping.config.Settings
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.multitenancy.MultiTenancySettings
import org.grails.datastore.mapping.multitenancy.exceptions.TenantNotFoundException
import org.grails.datastore.mapping.multitenancy.resolvers.SystemPropertyTenantResolver
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by graemerocher on 16/01/2017.
 */
class CurrentTenantTransformSpec  extends Specification {

    @Shared @AutoCleanup SimpleMapDatastore datastore = new SimpleMapDatastore(
            DatastoreUtils.createPropertyResolver(
                    (Settings.SETTING_MULTI_TENANCY_MODE): MultiTenancySettings.MultiTenancyMode.DATABASE,
                    (Settings.SETTING_MULTI_TENANT_RESOLVER): new SystemPropertyTenantResolver()
            ),
            [ConnectionSource.DEFAULT, 'two'],
            Team
    )

    void "Test @CurrentTenant with service"() {
        given:"A service with @CurrentTenant at the class level is used"
        TeamService teamService = new TeamService()

        when:"a method is invoked"
        def results = teamService.listTeams()

        then:"An exception is thrown because no tenant is present"
        thrown(TenantNotFoundException)

        when:"A tenant is set"
        System.setProperty(SystemPropertyTenantResolver.PROPERTY_NAME, "two")
        results = teamService.listTeams()

        then:"The result is correct"
        results.size() == 0

        when:"A @Transactional method is invoked"
        teamService.addTeam("test")
        results = teamService.listTeams()

        then:"The results are correct"
        results.size() == 1

        when:"we try with no tenant"
        System.setProperty(SystemPropertyTenantResolver.PROPERTY_NAME, "")
        teamService.addTeam("test 2")

        then:"An exception is thrown"
        thrown(TenantNotFoundException)
    }
}

@CurrentTenant
class TeamService {

    List<Team> listTeams() {
        Team.list()
    }

    @Transactional
    void addTeam(String name) {
        new Team(name:name).save(flush:true)
    }
}
