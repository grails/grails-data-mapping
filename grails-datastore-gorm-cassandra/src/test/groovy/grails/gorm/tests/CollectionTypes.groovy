package grails.gorm.tests

import grails.persistence.Entity

@Entity
class CollectionTypes {

    String string
    int i
    List<Integer> list
    HashSet<String> set
    Map<String, Float> map
}
