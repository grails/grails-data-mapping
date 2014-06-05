package grails.gorm.tests


/**
 * @author graemerocher
 */
class CollectionTypesPersistenceSpec extends GormDatastoreSpec {

    @Override
    public List getDomainClasses() {
        [CollectionTypes]
    }
    
    def testPersistCollectionTypes() {
        given:
            def list = [1,2,3]            
            def set = ["one", "two", "three"] as Set
            def map = [a:1, b:2.5f, z:3l]
            def ct = new CollectionTypes(string: "1", i:2, list: list, map: map, set:set)
        when:
            ct.save(flush:true)
            ct.discard()
            ct = CollectionTypes.get(ct.id)

        then:
            ct   
            "1" == ct.string
            2 == ct.i 
            list.equals(ct.list)
            set.equals(ct.set)
            map.equals(ct.map)        
    }
}

