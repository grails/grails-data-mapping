export GRADLE_OPTS="-Xmx2048m -Xms256m -XX:MaxPermSize=768m -XX:+CMSClassUnloadingEnabled -XX:+HeapDumpOnOutOfMemoryError" 


EXIT_STATUS=0
#./gradlew grails-datastore-gorm-hibernate:test || EXIT_STATUS=$?
#./gradlew grails-datastore-gorm-hibernate4:test || EXIT_STATUS=$?
#./gradlew grails-datastore-gorm-mongodb:test || EXIT_STATUS=$?
#./gradlew grails-datastore-gorm-redis:test || EXIT_STATUS=$?
#./gradlew grails-datastore-gorm-test:test || EXIT_STATUS=$?


version=$(grep 'projectVersion =' build.gradle)
version=${version//[[:blank:]]/}
version="${version#*=}";
version=${version//\"/}

releaseType=$(grep 'releaseType =' build.gradle | egrep -v ^[[:blank:]]*\/\/ | egrep -v ^[[:blank:]]*isBuildSnapshot)
releaseType=${releaseType//[[:blank:]]/}
releaseType="${releaseType#*=}";
releaseType=${releaseType//\"/}

echo "Project Version: $version $releaseType"

if [[ $releaseType != *-SNAPSHOT* ]]
then
    ./gradlew allDocs


    git config --global user.name "$GIT_NAME"
    git config --global user.email "$GIT_EMAIL"
    git config --global credential.helper "store --file=~/.git-credentials"
    echo "https://$GH_TOKEN:@github.com" > ~/.git-credentials

    git clone https://${GH_TOKEN}@github.com/grails/grails-data-mapping.git -b gh-pages gh-pages --single-branch > /dev/null
    cd gh-pages
    echo "Making directory for Version: $version"
    mkdir -p "$version"
    cd "$version"
    echo "Current Directory:"
    pwd

    git rm -rf .
    cp -r ../../build/docs/. ./
    git add *
    cd ..
    mkdir -p current
    cd current
    echo "Current Directory:"
    pwd
    git rm -rf .
    cp -r ../../build/docs/. ./
    git add *
    git commit -a -m "Updating docs for Travis build: https://travis-ci.org/grails/grails-data-mapping/builds/$TRAVIS_BUILD_ID"
    git push origin HEAD
    cd ../..
    rm -rf gh-pages
fi


exit $EXIT_STATUS



