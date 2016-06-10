#!/bin/bash
EXIT_STATUS=0

./gradlew --stop

if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
    echo "Tagged Release Skipping Tests for Publish"
else
    case "$GORM_IMPL"  in
        hibernate)
            ./gradlew grails-datastore-gorm-hibernate:test -no-daemon  || EXIT_STATUS=$?
            ;;
        hibernate4)
            ./gradlew grails-datastore-gorm-hibernate4:test -no-daemon  --stacktrace || EXIT_STATUS=$?
            if [[ $EXIT_STATUS -eq 0 ]]; then
                ./gradlew grails2-plugins/hibernate4:test || EXIT_STATUS=$?
            fi
            if [[ $EXIT_STATUS -eq 0 ]]; then
                ./gradlew boot-plugins/gorm-hibernate4-spring-boot:test || EXIT_STATUS=$?
            fi
            ;;
        hibernate5)
            ./gradlew grails-datastore-gorm-hibernate5:test -no-daemon  --stacktrace || EXIT_STATUS=$?
            ;;
        testgrails2)
            ./gradlew grails-datastore-gorm-grails2-test:test -no-daemon  || EXIT_STATUS=$?
            ;;
        *)

        # Run unit testing API tests
        if [[ $EXIT_STATUS -eq 0 ]]; then
            ./gradlew grails-datastore-gorm:test grails-datastore-gorm-test:test || EXIT_STATUS=$?
        fi
        ;;
    esac

fi

./gradlew --stop
./travis-publish.sh || EXIT_STATUS=$?

exit $EXIT_STATUS



