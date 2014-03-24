package grails.gorm.tests;

import grails.persistence.Entity;

import java.io.Serializable;
import java.util.UUID;

@Entity
class Book implements Serializable {
    UUID id
    Long version
    String author
    String title
    Boolean published = false

    static mapping = {
        published index:true
        title index:true
        author index:true
    }
}