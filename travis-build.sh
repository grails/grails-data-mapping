export GRADLE_OPTS="-Xmx2048m -Xms256m -XX:MaxPermSize=768m -XX:+CMSClassUnloadingEnabled -XX:+HeapDumpOnOutOfMemoryError" 


EXIT_STATUS=0
./gradlew grails-datastore-gorm-hibernate:test || EXIT_STATUS=$?
./gradlew grails-datastore-gorm-hibernate4:test || EXIT_STATUS=$?
./gradlew grails-datastore-gorm-mongodb:test || EXIT_STATUS=$?
./gradlew grails-datastore-gorm-test:test || EXIT_STATUS=$?
exit $EXIT_STATUS



