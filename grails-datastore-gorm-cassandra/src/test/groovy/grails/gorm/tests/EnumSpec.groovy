package grails.gorm.tests


import org.grails.datastore.mapping.core.Session

import spock.lang.Issue

class EnumSpec extends GormDatastoreSpec {

    void "Test save()"() {
        given:

            EnumThingEnumPartitionKey t = new EnumThingEnumPartitionKey(name: 'e1', en: TestEnum.V1)
            EnumThing p = new EnumThing(name: 'e2', en: TestEnum.V2)

        when:
            t.save(failOnError: true, flush:true)
            p.save(failOnError: true, flush:true)
            session.clear()
        then:
            t != null
            !t.hasErrors()
            
            p != null
            !p.hasErrors()

        when:
            t = t.get([en:t.en])
            p = p.get(p.id)

        then:
            t != null
            'e1' == t.name
            TestEnum.V1 == t.en
            
            p != null
            'e2' == p.name
            TestEnum.V2 == p.en                       
    }


    @Issue('GPMONGODB-248')
    void "Test findByInList()"() {
        given:

        new EnumThingEnumPartitionKey(name: 'e1', en: TestEnum.V1).save(failOnError: true)
        new EnumThingEnumPartitionKey(name: 'e2', en: TestEnum.V1).save(failOnError: true)
        new EnumThingEnumPartitionKey(name: 'e3', en: TestEnum.V2).save(failOnError: true)

        EnumThingEnumPartitionKey instance1
        EnumThingEnumPartitionKey instance2
        EnumThingEnumPartitionKey instance3

        when:
        instance1 = EnumThingEnumPartitionKey.findByEnInList([TestEnum.V1])
        instance2 = EnumThingEnumPartitionKey.findByEnInList([TestEnum.V2])
        instance3 = EnumThingEnumPartitionKey.findByEnInList([TestEnum.V3])

        then:
        instance1 != null
        instance1.en == TestEnum.V1

        instance2 != null
        instance2.en == TestEnum.V2

        instance3 == null
    }
    
    void "Test findBy()"() {
        given:

            new EnumThingEnumPartitionKey(name: 'e1', en: TestEnum.V1).save(failOnError: true)
            new EnumThingEnumPartitionKey(name: 'e2', en: TestEnum.V1).save(failOnError: true)
            new EnumThingEnumPartitionKey(name: 'e3', en: TestEnum.V2).save(failOnError: true)

            EnumThingEnumPartitionKey instance1
            EnumThingEnumPartitionKey instance2
            EnumThingEnumPartitionKey instance3

        when:
            instance1 = EnumThingEnumPartitionKey.findByEn(TestEnum.V1)
            instance2 = EnumThingEnumPartitionKey.findByEn(TestEnum.V2)
            instance3 = EnumThingEnumPartitionKey.findByEn(TestEnum.V3)

        then:
            instance1 != null
            instance1.en == TestEnum.V1

            instance2 != null
            instance2.en == TestEnum.V2

            instance3 == null
    }

    void "Test findBy() with clearing the session"() {
        given:

            new EnumThingEnumPartitionKey(name: 'e1', en: TestEnum.V1).save(failOnError: true, flush: true)
            new EnumThingEnumPartitionKey(name: 'e2', en: TestEnum.V1).save(failOnError: true, flush: true)
            new EnumThingEnumPartitionKey(name: 'e3', en: TestEnum.V2).save(failOnError: true, flush: true)
            session.clear()

            EnumThingEnumPartitionKey instance1
            EnumThingEnumPartitionKey instance2
            EnumThingEnumPartitionKey instance3

        when:
            instance1 = EnumThingEnumPartitionKey.findByEn(TestEnum.V1)
            instance2 = EnumThingEnumPartitionKey.findByEn(TestEnum.V2)
            instance3 = EnumThingEnumPartitionKey.findByEn(TestEnum.V3)

        then:
            instance1 != null
            instance1.en == TestEnum.V1

            instance2 != null
            instance2.en == TestEnum.V2

            instance3 == null
    }

    void "Test findAllBy()"() {
        given:

            new EnumThingEnumPartitionKey(name: 'e1', en: TestEnum.V1).save(failOnError: true)
            new EnumThingEnumPartitionKey(name: 'e2', en: TestEnum.V1).save(failOnError: true)
            new EnumThingEnumPartitionKey(name: 'e3', en: TestEnum.V2).save(failOnError: true)

            def v1Instances
            def v2Instances
            def v3Instances
            def v12Instances

        when:
            v1Instances = EnumThingEnumPartitionKey.findAllByEn(TestEnum.V1)
            v2Instances = EnumThingEnumPartitionKey.findAllByEn(TestEnum.V2)
            v3Instances = EnumThingEnumPartitionKey.findAllByEn(TestEnum.V3)
            v12Instances = EnumThingEnumPartitionKey.findAllByEnInList([TestEnum.V1, TestEnum.V2])

        then:
            v1Instances != null
            v1Instances.size() == 2
            v1Instances.every { it.en == TestEnum.V1 }

            v2Instances != null
            v2Instances.size() == 1
            v2Instances.every { it.en == TestEnum.V2 }

            v3Instances != null
            v3Instances.isEmpty()

            v12Instances != null
            v12Instances.size() == 3
    }

    void "Test countBy()"() {
        given:

            new EnumThingEnumPartitionKey(name: 'e1', en: TestEnum.V1).save(failOnError: true)
            new EnumThingEnumPartitionKey(name: 'e2', en: TestEnum.V1).save(failOnError: true)
            new EnumThingEnumPartitionKey(name: 'e3', en: TestEnum.V2).save(failOnError: true)

            def v1Count
            def v2Count
            def v3Count

        when:
            v1Count = EnumThingEnumPartitionKey.countByEn(TestEnum.V1)
            v2Count = EnumThingEnumPartitionKey.countByEn(TestEnum.V2)
            v3Count = EnumThingEnumPartitionKey.countByEn(TestEnum.V3)

        then:
            2 == v1Count
            1 == v2Count
            0 == v3Count
    }
}
