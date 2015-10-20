#!/bin/bash
EXIT_STATUS=0

case "$GORM_IMPL"  in
    hibernate)
        ./gradlew grails-datastore-gorm-hibernate:test || EXIT_STATUS=$?
        ;;
    hibernate4)
        ./gradlew grails-datastore-gorm-hibernate4:test --stacktrace || EXIT_STATUS=$?
        ;;
    mongodb)
        ./gradlew grails-datastore-gorm-mongodb:test --stacktrace || EXIT_STATUS=$?
        ;;
    redis)
        ./gradlew grails-datastore-gorm-redis:test || EXIT_STATUS=$?
        ;;
    cassandra)
        # wait for Cassandra to start up
        sleep 5
        ./gradlew grails-datastore-gorm-cassandra:test || EXIT_STATUS=$?
        ;;
    neo4j)
        ./gradlew --stop
        ./gradlew grails-datastore-gorm-neo4j:test -no-daemon -i || EXIT_STATUS=$?
        ;;
    restclient)
        ./gradlew grails-datastore-gorm-rest-client:test || EXIT_STATUS=$?
        ;;
    *)
        ./gradlew testClasses || EXIT_STATUS=$?

        # Run Grails 2 plugin smoke test
        ./gradlew grails2-plugins/mongodb:test
        ./gradlew grails-datastore-gorm:test grails-datastore-gorm-test:test || EXIT_STATUS=$?
        ;;
esac

./travis-publish.sh || EXIT_STATUS=$?

exit $EXIT_STATUS



