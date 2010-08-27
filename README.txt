Grails Inconsequential
----------------------

Building
--------

To build the project use the Gradle build. You do not need Gradle installed to do this.

Simply type:

./gradlew assemble

Requirements
------------

Cassandra
---------

The Cassandra module requires Hector to be installed into your local Maven cache.

Hector does not at this time exist in a public Maven repository.

To do so execute the following commands:

git clone git://github.com/rantav/hector.git
cd hector
mvn -DskipTests=true install


Redis
-----

The Redis module requires Jedis to be installed into your
local Maven cache.

Then do the following for Jedis:

git clone git://github.com/graemerocher/jedis.git
cd jedis
gradle install

