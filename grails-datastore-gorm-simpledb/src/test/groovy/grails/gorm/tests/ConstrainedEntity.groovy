package grails.gorm.tests

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 8/4/11
 * Time: 1:57 PM
 * To change this template use File | Settings | File Templates.
 */
class ConstrainedEntity implements Serializable{
    static final MAX_VALUE = 1000
    static final List<String> ALLOWABLE_VALUES=['ABC','DEF','GHI']

    String id
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