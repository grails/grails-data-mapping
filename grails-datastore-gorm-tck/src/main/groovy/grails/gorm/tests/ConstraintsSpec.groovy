package grails.gorm.tests

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 8/4/11
 * Time: 10:59 AM
 * To change this template use File | Settings | File Templates.
 */
class ConstraintsSpec extends GormDatastoreSpec{

    static {
        TEST_CLASSES <<  ConstrainedEntity
    }
    void "Test constraints with static default values"() {
         given: "A Test class with static constraint values"
            def ce = new ConstrainedEntity(num:1000, str:"ABC")

         when: "saved is called"
            ce.save()


         then:
            ce.hasErrors() == false

    }
}
class ConstrainedEntity {
    static final MAX_VALUE = 1000
    static final List<String> ALLOWABLE_VALUES=['ABC','DEF','GHI']

    Long id
    Integer num
    String str

    static constraints = {

        num(maxSize:MAX_VALUE) /*Must be MyDomainClass.MAX_VALUE in order work with redis*/
        str(validator:{val, obj ->
				if(val != null && !ALLOWABLE_VALUES.contains(val)) {/*Must be MyDomainClass.ALLOWABLE_VALUES in order work with redis */
					return ['not.valid']
                }
            }
          )
    }

}
