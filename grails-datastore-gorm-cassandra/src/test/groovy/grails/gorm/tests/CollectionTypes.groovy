package grails.gorm.tests

import grails.gorm.CassandraEntity

@CassandraEntity
class CollectionTypes {

    String string
    int i
    List<Integer> list
    HashSet<String> set
    Map<String, Float> map
}
