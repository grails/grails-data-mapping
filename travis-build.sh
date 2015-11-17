#!/bin/bash
EXIT_STATUS=0

./gradlew --stop

case "$GORM_IMPL"  in
    hibernate)
        ./gradlew grails-datastore-gorm-hibernate:test -no-daemon  || EXIT_STATUS=$?
        ;;
    hibernate4)
        ./gradlew grails-datastore-gorm-hibernate4:test -no-daemon  --stacktrace || EXIT_STATUS=$?
        ;;
    hibernate5)
        ./gradlew grails-datastore-gorm-hibernate5:test -no-daemon  --stacktrace || EXIT_STATUS=$?
        ;;
    mongodb)
        ./gradlew grails-datastore-gorm-mongodb:test -no-daemon --stacktrace || EXIT_STATUS=$?
        if [[ $EXIT_STATUS -eq 0 ]]; then
            ./gradlew grails2-plugins/mongodb:test || EXIT_STATUS=$?
        fi
        ;;
    redis)
        ./gradlew grails-datastore-gorm-redis:test -no-daemon  || EXIT_STATUS=$?
        ;;
    testgrails2)
        ./gradlew grails-datastore-gorm-grails2-test:test -no-daemon  || EXIT_STATUS=$?
        ;;
    cassandra)
        # wait for Cassandra to start up
        sleep 5
        ./gradlew grails-datastore-gorm-cassandra:test -no-daemon  || EXIT_STATUS=$?
        ;;
    neo4j)
        ./gradlew grails-datastore-gorm-neo4j:test -no-daemon  || EXIT_STATUS=$?
        if [[ $EXIT_STATUS -eq 0 ]]; then
            ./gradlew grails2-plugins/neo4j:test || EXIT_STATUS=$?
        fi
        ;;
    restclient)
        ./gradlew grails-datastore-gorm-rest-client:test -no-daemon  || EXIT_STATUS=$?
        ;;
    *)

        # Run unit testing API tests
        if [[ $EXIT_STATUS -eq 0 ]]; then
            ./gradlew grails-datastore-test-support:test || EXIT_STATUS=$?
        fi
        if [[ $EXIT_STATUS -eq 0 ]]; then
            ./gradlew grails-datastore-gorm:test grails-datastore-gorm-test:test || EXIT_STATUS=$?
        fi
        ;;
esac

./travis-publish.sh || EXIT_STATUS=$?

exit $EXIT_STATUS



