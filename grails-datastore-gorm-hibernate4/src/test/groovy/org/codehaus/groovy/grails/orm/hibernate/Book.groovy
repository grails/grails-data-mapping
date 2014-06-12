package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

@Entity
class Book {
    Long id
    Long version

    String title
    Popularity popularity

    static embedded = ["popularity"]

    static namedQueries = {
        popularBooks {
            popularity {
                gt "liked", 0
            }
        }
    }
}

class Popularity {
    int liked
}

@Entity
class OneBookAuthor {
    Long id
    Long version

    Book book
}

@Entity
class OneAuthorPublisher {
    Long id
    Long version

    String name
    OneBookAuthor author

    static embedded = ['author']

    static namedQueries = {
        withPopularBooks {
            author {
                book {
                    popularity {
                        gt 'liked', 0
                    }
                }
            }
        }
    }
}