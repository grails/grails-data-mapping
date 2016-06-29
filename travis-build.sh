#!/bin/bash
EXIT_STATUS=0

./gradlew --stop

if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
    echo "Tagged Release Skipping Tests for Publish"
else
    ./gradlew compileGroovy
    ./gradlew --stop
    ./gradlew compileTestGroovy
    ./gradlew --stop
    ./gradlew --refresh-dependencies grails-datastore-gorm-hibernate4:test || EXIT_STATUS=$?

    if [[ $EXIT_STATUS -eq 0 ]]; then
        ./gradlew --stop
        ./gradlew check -x grails-datastore-gorm-hibernate4:test || EXIT_STATUS=$?
    fi
fi

if [[ $EXIT_STATUS -eq 0 ]]; then
    ./gradlew --stop
    ./travis-publish.sh || EXIT_STATUS=$?
fi

exit $EXIT_STATUS



