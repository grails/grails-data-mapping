package grails.gorm.tests



import grails.persistence.Entity

@Entity
class PublicationTitlePartitionKey implements Serializable {    
    Long version
    String title
    Date datePublished
    Boolean paperback = true

    static mapping = {
        id name:"title", primaryKey:[ordinal:0, type:"partitioned"], generator:"assigned"                 
        datePublished primaryKey:[ordinal:1, type: "clustered"]         
        paperback index:true        
        
    }

    static namedQueries = {

        lastPublishedBefore { date ->
            uniqueResult = true
            le 'datePublished', date
            order 'datePublished', 'desc'
        }

        recentPublications {
            def now = new Date()
            gt 'datePublished', now - 365
        }

        publicationsByTitles { titles ->
            'in' 'title', titles
        }
        
        publicationsByTitle { title ->
            eq 'title', title
        }

        recentPublicationsByTitle { title ->
            recentPublications()
            eq 'title', title
        }

        latestBooks {
            maxResults(10)
            order("datePublished", "desc")
        }

        publishedBetween { start, end ->
            between 'datePublished', start, end
        }

        publishedAfter { date ->
            gt 'datePublished', date
        }

        paperbackOrRecent {
            or {
                def now = new Date()
                gt 'datePublished', now - 365
                paperbacks()
            }
        }

        paperbacks {
            eq 'paperback', true
        }

        paperbackAndRecent {
            paperbacks()
            recentPublications()
        }

        thisWeeksPaperbacks() {
            paperbacks()
            def today = new Date()
            publishedBetween(today - 7, today)
        }
    }
}