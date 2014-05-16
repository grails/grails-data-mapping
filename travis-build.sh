export GRADLE_OPTS="-Xmx2048m -Xms256m -XX:MaxPermSize=768m -XX:+CMSClassUnloadingEnabled -XX:+HeapDumpOnOutOfMemoryError" 


./gradlew grails-datastore-gorm-hibernate:test
./gradlew grails-datastore-gorm-hibernate4:test
./gradlew grails-datastore-gorm-mongodb:test
./gradlew grails-datastore-gorm-test:test