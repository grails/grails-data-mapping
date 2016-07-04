#!/bin/bash

echo "Publishing..."

EXIT_STATUS=0

if [[ $TRAVIS_REPO_SLUG == "grails/grails-data-mapping" && $TRAVIS_PULL_REQUEST == 'false' && $EXIT_STATUS -eq 0 ]]; then

  echo "Publishing archives"
  echo "org.gradle.jvmargs=-XX\:MaxPermSize\=1024m -Xmx1500m -Dfile.encoding\=UTF-8 -Duser.country\=US -Duser.language\=en -Duser.variant" >> ~/.gradle/gradle.properties
  echo "org.gradle.daemon=false" >> ~/.gradle/gradle.properties
  ./gradlew --stop

  export GRADLE_OPTS="-XX:MaxPermSize=1024m -Xmx1500m -Dfile.encoding=UTF-8"

  gpg --keyserver keyserver.ubuntu.com --recv-key $SIGNING_KEY
  if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
    # for releases we upload to Bintray and Sonatype OSS
    ./gradlew --stop
    ./gradlew -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" uploadArchives || EXIT_STATUS=$?

    if [[ $EXIT_STATUS -eq 0 ]]; then
        ./gradlew --stop
        ./gradlew -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" publish || EXIT_STATUS=$?
    fi

    if [[ $EXIT_STATUS -eq 0 ]]; then
        ./gradlew --stop
        ./gradlew -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" bintrayUpload || EXIT_STATUS=$?
    fi
    

  else
    # for snapshots only to repo.grails.org
    ./gradlew --stop
    ./gradlew -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" publish  || EXIT_STATUS=$?
  fi

  if [[ $EXIT_STATUS -eq 0 ]]; then
      echo "Trigger Travis Functional Test build"
      ./trigger-dependent-build.sh


        git config --global user.name "$GIT_NAME"
        git config --global user.email "$GIT_EMAIL"
        git config --global credential.helper "store --file=~/.git-credentials"
        echo "https://$GH_TOKEN:@github.com" > ~/.git-credentials

        echo "Triggering Hibernate 5 build"
        git clone https://${GH_TOKEN}@github.com/grails/gorm-hibernate5.git gorm-hibernate5
        cd gorm-hibernate5
        echo "$(date)" > .snapshot
        git add .snapshot
        if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
            echo "gormVersion=${TRAVIS_TAG:1}" > gradle.properties
            git add gradle.properties
            git commit -m "Release GORM for Hibernate 5 $TRAVIS_TAG"
            git tag $TRAVIS_TAG
            git push --tags
        else
            git commit -m "New Core Snapshot: $(date)"
        fi
        git push
        cd ..

        echo "Triggering Hibernate 4 build"
        git clone https://${GH_TOKEN}@github.com/grails/gorm-hibernate4.git gorm-hibernate4
        cd gorm-hibernate4
        echo "$(date)" > .snapshot
        git add .snapshot
        if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
            echo "gormVersion=${TRAVIS_TAG:1}" > gradle.properties
            git add gradle.properties
            git commit -m "Release GORM for Hibernate 4 $TRAVIS_TAG"
            git tag $TRAVIS_TAG
            git push --tags
        else
            git commit -m "New Core Snapshot: $(date)"
        fi
        git push
        cd ..


        echo "Triggering REST Client build"
        git clone https://${GH_TOKEN}@github.com/grails/gorm-rest-client.git gorm-rest-client
        cd gorm-rest-client
        echo "$(date)" > .snapshot
        git add .snapshot

        if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
            echo "gormVersion=${TRAVIS_TAG:1}" > gradle.properties
            git add gradle.properties
            git commit -m "New GORM Release $TRAVIS_TAG"
        else
            git commit -m "New Core Snapshot: $(date)"
        fi
        git push
        cd ..

        echo "Triggering Neo4j build"
        git clone https://${GH_TOKEN}@github.com/grails/gorm-neo4j.git gorm-neo4j
        cd gorm-neo4j
        echo "$(date)" > .snapshot
        git add .snapshot
        if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
            echo "gormVersion=${TRAVIS_TAG:1}" > gradle.properties
            git add gradle.properties
            git commit -m "Release GORM for Neo4j $TRAVIS_TAG"
            git tag $TRAVIS_TAG
            git push --tags
        else
            git commit -m "New Core Snapshot: $(date)"
        fi
        git push
        cd ..

        echo "Triggering MongoDB build"
        git clone https://${GH_TOKEN}@github.com/grails/gorm-mongodb.git gorm-mongodb
        cd gorm-mongodb
        echo "$(date)" > .snapshot
        git add .snapshot
        if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
            echo "gormVersion=${TRAVIS_TAG:1}" > gradle.properties
            git add gradle.properties
            git commit -m "Release GORM for MongoDB $TRAVIS_TAG"
            git tag $TRAVIS_TAG
            git push --tags
        else
            git commit -m "New Core Snapshot: $(date)"
        fi
        git push
        cd ..

        echo "Triggering Redis build"
        git clone https://${GH_TOKEN}@github.com/grails/gorm-redis.git gorm-redis
        cd gorm-redis
        echo "$(date)" > .snapshot
        git add .snapshot
        if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
            echo "gormVersion=${TRAVIS_TAG:1}" > gradle.properties
            git add gradle.properties
            git commit -m "Release GORM for Redis $TRAVIS_TAG"
            git tag $TRAVIS_TAG
            git push --tags
        else
            git commit -m "New Core Snapshot: $(date)"
        fi
        git push
        cd ..

        echo "Triggering Cassandra build"
        git clone https://${GH_TOKEN}@github.com/grails/gorm-cassandra.git gorm-cassandra
        cd gorm-cassandra
        echo "$(date)" > .snapshot
        git add .snapshot
        if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
            echo "gormVersion=${TRAVIS_TAG:1}" > gradle.properties
            git add gradle.properties
            git commit -m "Release GORM for Cassandra $TRAVIS_TAG"
            git tag $TRAVIS_TAG
            git push --tags
        else
            git commit -m "New Core Snapshot: $(date)"
        fi
        git push
        cd ..

        # If there is a tag present then this becomes the latest
        if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
            echo "Triggering documentation build"
            git clone https://${GH_TOKEN}@github.com/grails/gorm-docs.git gorm-docs
            cd gorm-docs

            echo "gormVersion=${TRAVIS_TAG:1}" > gradle.properties
            git add gradle.properties
            git commit -m "Release $TRAVIS_TAG docs"
            git tag $TRAVIS_TAG
            git push --tags
            git push
            cd ..
        fi

  else
      echo "Error occured during publishing, skipping docs"
  fi

fi

if [[ $EXIT_STATUS -eq 0 ]]; then
  echo "Publishing Successful."
fi
exit $EXIT_STATUS
