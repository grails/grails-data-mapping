package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class ConstrainedEntity implements Serializable {

    static final MAX_VALUE = 1000
    static final List<String> ALLOWABLE_VALUES = ['ABC','DEF','GHI']

    Long id
    Integer num
    String str

    static constraints = {
         num(maxSize:MAX_VALUE) /*Must be MyDomainClass.MAX_VALUE in order work with redis*/
         str validator: { val, obj ->
             if (val != null && !ALLOWABLE_VALUES.contains(val)) {/*Must be MyDomainClass.ALLOWABLE_VALUES in order work with redis */
                 return ['not.valid']
             }
         }
     }
}
