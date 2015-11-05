[![Build Status](https://travis-ci.org/grails/grails-data-mapping.svg?branch=master)](https://travis-ci.org/grails/grails-data-mapping)

Grails Datastore API (aka GORM)
===

[Grails][Grails] is a framework used to build web applications with the [Groovy][Groovy] programming language. This project provides the plumbings for the GORM API both for Hibernate and for new implementations of GORM ontop of NoSQL datastores.
[Grails]: http://grails.org/
[Groovy]: http://groovy-lang.org/


Getting Started
---

See the following links for documentation on the various implementations:

* [GORM for Hibernate](http://grails.org/doc/latest/guide/GORM.html)
* [GORM for MongoDB](http://grails.github.io/grails-data-mapping/latest/mongodb/index.html)
* [GORM for Neo4j](http://grails.github.io/grails-data-mapping/snapshot/neo4j/index.html)
 
For API documentation see:

* [Core API / GORM for Hibernate](http://grails.github.io/grails-data-mapping/latest/api)
* [GORM for MongoDB API](http://grails.github.io/grails-data-mapping/latest/mongodb/api/index.html)
* [GORM for Neo4j API](http://grails.github.io/grails-data-mapping/snapshot/neo4j/api/index.html)

For other implementations see the [following page](http://grails.github.io/grails-data-mapping/latest).

Below is an example of using GORM for Hibernate in a Groovy script:

```groovy
@Grab("org.grails:grails-datastore-gorm-hibernate4:3.0.0.RELEASE")
@Grab("org.grails:grails-spring:2.3.6")
@Grab("com.h2database:h2:1.3.164")
import grails.orm.bootstrap.*
import grails.persistence.*
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.h2.Driver
 
 
init = new HibernateDatastoreSpringInitializer(Person)
def dataSource = new DriverManagerDataSource(Driver.name, "jdbc:h2:prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE", 'sa', '')
init.configureForDataSource(dataSource) 
 
 
println "Total people = " + Person.count()
 
 
@Entity
class Person {
    String name
    static constraints = {
        name blank:false
    }
}
```



Developing Implementations
---

For further information on the project see the comprehensive [developer guide][Developer Guide].
[Developer Guide]: http://grails.github.io/grails-data-mapping/latest/manual/index.html
	
License
---

Grails and Groovy are licensed under the terms of the [Apache License, Version 2.0][Apache License, Version 2.0].
[Apache License, Version 2.0]: http://www.apache.org/licenses/LICENSE-2.0.html
