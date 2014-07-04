package grails.gorm.tests

import grails.gorm.CassandraEntity



@CassandraEntity
class ConstrainedEntity implements Serializable {

	static final int MAX_VALUE = 1000
	static final List<String> ALLOWABLE_VALUES = ['ABC','DEF','GHI']

	UUID id
	Integer num
	String str

	static constraints = {

		num(maxSize: MAX_VALUE) /*Must be MyDomainClass.MAX_VALUE in order work with redis*/
		str validator: { val, obj ->
			if (val != null && !ALLOWABLE_VALUES.contains(val)) {/*Must be MyDomainClass.ALLOWABLE_VALUES in order work with redis */
				return ['not.valid']
			}
		}
	}
}