package grails.gorm.tests

import spock.lang.Specification
import org.grails.datastore.mapping.dynamodb.util.DynamoDBUtil

/**
 * Tests combination method in dymanodb util.
 */
class DynamoDBCombinationSpec extends Specification {
    void "Test 1"() {
        given:
            def input = [
                    [ ['a', 'b'] ]
            ]

        when:
            def result = DynamoDBUtil.combinate(input);

        then:
            result == [ ['a', 'b'] ]
    }
    void "Test 2"() {
        given:
        def input = [
                [ ['a'], ['b'] ]
        ]

        when:
            def result = DynamoDBUtil.combinate(input);

        then:
            result == [ ['a'], ['b'] ]
    }
    void "Test 3"() {
        given:
        def input = [
                [ ['a'], ['b'] ],
                [ ['c'], ['d'] ],
        ]

        when:
            def result = DynamoDBUtil.combinate(input);

        then:
            result == [ ['c', 'a'], ['d', 'a'], ['c', 'b'], ['d', 'b']]
    }
    void "Test 4"() {
        given:
        def input = [
                [ ['a'], ['b'] ],
                [ ['c'], ['d'] ],
                [ ['e'] ],
        ]

        when:
            def result = DynamoDBUtil.combinate(input);

        then:
            result == [['e', 'c', 'a'], ['e', 'd', 'a'], ['e', 'c', 'b'], ['e', 'd', 'b']]
    }
}