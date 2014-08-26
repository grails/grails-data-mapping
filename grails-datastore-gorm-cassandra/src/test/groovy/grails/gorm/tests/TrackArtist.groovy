package grails.gorm.tests

import grails.persistence.Entity

@Entity
class TrackArtist {

    UUID trackId
    String track
    String artist
    Integer trackLengthSeconds
    String genre
    String musicFile
    Boolean starred

    static mapping = {
        table "track_by_artist"
        id name:"artist", primaryKey:[ordinal:0, type: "partitioned"], generator:"assigned"
        track primaryKey:[ordinal:1, type: "clustered"]
        trackId primaryKey:[ordinal:2, type:"clustered"], column:"track_id"
        trackLengthSeconds column:"track_length_in_seconds"
        musicFile column:"music_file"
        version false
    }

    static constraints = {
        starred nullable:true
    }
}
