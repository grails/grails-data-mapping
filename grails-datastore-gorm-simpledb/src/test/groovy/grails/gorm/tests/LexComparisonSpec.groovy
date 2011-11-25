package grails.gorm.tests

/**
 * Tests correctness of lexicographical comparisons and encoding.
 *
 * @author Roman Stepanenko
 */
class LexComparisonSpec extends GormDatastoreSpec {
    @Override
    List getDomainClasses() {
        [CommonTypes]
    }


    void "Test byte comparison"() {
        given:
            create("Alex"){ it.setB(Byte.MAX_VALUE) }.save()
            create("Bob"){ it.setB(Byte.MIN_VALUE) }.save()
            create("Sam"){ it.setB(-102 as byte) }.save()
            create("Carl"){ it.setB(-15 as byte) }.save()
            create("Don"){ it.setB(-13 as byte) }.save()
            create("Earl"){ it.setB(0 as byte) }.save()
            create("Roman"){ it.setB(15 as byte) }.save()
            create("Glen"){ it.setB(125 as byte) }.save()
            session.flush()
            session.clear()

            def criteria = CommonTypes.createCriteria()

        when:
            def results = CommonTypes.findAllByB(Byte.MAX_VALUE)

        then:
            results.size() == 1
            results[0].name == 'Alex'

        when:
            results = CommonTypes.findAllByB(Byte.MIN_VALUE)

        then:
            results.size() == 1
            results[0].name == 'Bob'

        when:
            results = CommonTypes.findAllByB(-13 as byte)

        then:
            results.size() == 1
            results[0].name == 'Don'

        when:
            results = CommonTypes.findAllByB(125 as byte)

        then:
            results.size() == 1
            results[0].name == 'Glen'

        when:
            results = criteria.list {
                gt('b', 0 as byte)
                order("b", "asc")
            }

        then:
            results.size() == 3
            results.collect {it.name} == ["Roman", "Glen", "Alex"]

        when:
            results = criteria.list {
                gt('b', -14 as byte)
                order("b", "asc")
            }

        then:
            results.size() == 5
            results.collect {it.name} == ["Don", "Earl", "Roman", "Glen", "Alex"]

        when:
            results = criteria.list {
                lt('b', 0 as byte)
                order("b", "asc")
            }

        then:
            results.size() == 4
            results.collect {it.name} == ["Bob", "Sam", "Carl", "Don"]

        when:
            results = criteria.list {
                lt('b', 2 as byte)
                order("b", "desc")
            }

        then:
            results.size() == 5
            results.collect {it.name} == ["Earl", "Don", "Carl", "Sam", "Bob"]
    }

    void "Test short comparison"() {
        given:
            create("Alex"){ it.setS(Short.MAX_VALUE) }.save()
            create("Bob"){ it.setS(Short.MIN_VALUE) }.save()
            create("Sam"){ it.setS(-102 as short) }.save()
            create("Carl"){ it.setS(-15 as short) }.save()
            create("Don"){ it.setS(-13 as short) }.save()
            create("Earl"){ it.setS(0 as short) }.save()
            create("Roman"){ it.setS(15 as short) }.save()
            create("Glen"){ it.setS(125 as short) }.save()
            session.flush()
            session.clear()

            def criteria = CommonTypes.createCriteria()

        when:
            def results = CommonTypes.findAllByS(Short.MAX_VALUE)

        then:
            results.size() == 1
            results[0].name == 'Alex'

        when:
            results = CommonTypes.findAllByS(Short.MIN_VALUE)

        then:
            results.size() == 1
            results[0].name == 'Bob'

        when:
            results = CommonTypes.findAllByS(-13 as short)

        then:
            results.size() == 1
            results[0].name == 'Don'

        when:
            results = CommonTypes.findAllByS(125 as short)

        then:
            results.size() == 1
            results[0].name == 'Glen'

        when:
            results = criteria.list {
                gt('s', 0 as short)
                order("s", "asc")
            }

        then:
            results.size() == 3
            results.collect {it.name} == ["Roman", "Glen", "Alex"]

        when:
            results = criteria.list {
                gt('s', -14 as short)
                order("s", "asc")
            }

        then:
            results.size() == 5
            results.collect {it.name} == ["Don", "Earl", "Roman", "Glen", "Alex"]

        when:
            results = criteria.list {
                lt('s', 0 as short)
                order("s", "asc")
            }

        then:
            results.size() == 4
            results.collect {it.name} == ["Bob", "Sam", "Carl", "Don"]

        when:
            results = criteria.list {
                lt('s', 2 as short)
                order("s", "desc")
            }

        then:
            results.size() == 5
            results.collect {it.name} == ["Earl", "Don", "Carl", "Sam", "Bob"]
    }


    void "Test integer comparison"() {
        given:
            create("Alex"){ it.setI(Integer.MAX_VALUE) }.save()
            create("Bob"){ it.setI(Integer.MIN_VALUE) }.save()
            create("Sam"){ it.setI(-102 as int) }.save()
            create("Carl"){ it.setI(-15 as int) }.save()
            create("Don"){ it.setI(-13 as int) }.save()
            create("Earl"){ it.setI(0 as int) }.save()
            create("Roman"){ it.setI(15 as int) }.save()
            create("Glen"){ it.setI(125 as int) }.save()
            session.flush()
            session.clear()

            def criteria = CommonTypes.createCriteria()

        when:
            def results = CommonTypes.findAllByI(Integer.MAX_VALUE)

        then:
            results.size() == 1
            results[0].name == 'Alex'

        when:
            results = CommonTypes.findAllByI(Integer.MIN_VALUE)

        then:
            results.size() == 1
            results[0].name == 'Bob'

        when:
            results = CommonTypes.findAllByI(-13 as int)

        then:
            results.size() == 1
            results[0].name == 'Don'

        when:
            results = CommonTypes.findAllByI(125 as int)

        then:
            results.size() == 1
            results[0].name == 'Glen'

        when:
            results = criteria.list {
                gt('i', 0 as int)
                order("i", "asc")
            }

        then:
            results.size() == 3
            results.collect {it.name} == ["Roman", "Glen", "Alex"]

        when:
            results = criteria.list {
                gt('i', -14 as int)
                order("i", "asc")
            }

        then:
            results.size() == 5
            results.collect {it.name} == ["Don", "Earl", "Roman", "Glen", "Alex"]

        when:
            results = criteria.list {
                lt('i', 0 as int)
                order("i", "asc")
            }

        then:
            results.size() == 4
            results.collect {it.name} == ["Bob", "Sam", "Carl", "Don"]

        when:
            results = criteria.list {
                lt('i', 2 as int)
                order("i", "desc")
            }

        then:
            results.size() == 5
            results.collect {it.name} == ["Earl", "Don", "Carl", "Sam", "Bob"]
    }

    void "Test long comparison"() {
        given:
            create("Alex"){ it.setL(Long.MAX_VALUE) }.save()
            create("Bob"){ it.setL(Long.MIN_VALUE) }.save()
            create("Sam"){ it.setL(-102 as long) }.save()
            create("Carl"){ it.setL(-15 as long) }.save()
            create("Don"){ it.setL(-13 as long) }.save()
            create("Earl"){ it.setL(0 as long) }.save()
            create("Roman"){ it.setL(15 as long) }.save()
            create("Glen"){ it.setL(125 as long) }.save()
            session.flush()
            session.clear()

            def criteria = CommonTypes.createCriteria()

        when:
            def results = CommonTypes.findAllByL(Long.MAX_VALUE)

        then:
            results.size() == 1
            results[0].name == 'Alex'

        when:
            results = CommonTypes.findAllByL(Long.MIN_VALUE)

        then:
            results.size() == 1
            results[0].name == 'Bob'

        when:
            results = CommonTypes.findAllByL(-13 as long)

        then:
            results.size() == 1
            results[0].name == 'Don'

        when:
            results = CommonTypes.findAllByL(125 as long)

        then:
            results.size() == 1
            results[0].name == 'Glen'

        when:
            results = criteria.list {
                gt('l', 0 as long)
                order("l", "asc")
            }

        then:
            results.size() == 3
            results.collect {it.name} == ["Roman", "Glen", "Alex"]

        when:
            results = criteria.list {
                gt('l', -14 as long)
                order("l", "asc")
            }

        then:
            results.size() == 5
            results.collect {it.name} == ["Don", "Earl", "Roman", "Glen", "Alex"]

        when:
            results = criteria.list {
                lt('l', 0 as long)
                order("l", "asc")
            }

        then:
            results.size() == 4
            results.collect {it.name} == ["Bob", "Sam", "Carl", "Don"]

        when:
            results = criteria.list {
                lt('l', 2 as long)
                order("l", "desc")
            }

        then:
            results.size() == 5
            results.collect {it.name} == ["Earl", "Don", "Carl", "Sam", "Bob"]
    }

    private CommonTypes create(def name, def modifier){
        def now = new Date()
        def cal = new GregorianCalendar()
        def ct = new CommonTypes(
            name: name,
            l: 10L,
            b: 10 as byte,
            s: 10 as short,
            bool: true,
            i: 10,
            url: new URL("http://google.com"),
            date: now,
            c: cal,
            bd: 1.0,
            bi: 10 as BigInteger,
            d: 1.0 as Double,
            f: 1.0 as Float,
            tz: TimeZone.getTimeZone("GMT"),
            loc: Locale.UK,
            cur: Currency.getInstance("USD")
        )
        modifier(ct)
        print "returning: "+ct
        return ct
    }
}