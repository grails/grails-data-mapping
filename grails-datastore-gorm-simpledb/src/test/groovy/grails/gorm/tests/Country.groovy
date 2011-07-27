package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

@Entity
class Country extends Location {
    String id
    Integer population

    static hasMany = [residents: Person]
    Set residents

    public String toString() {
        return "Country{" +
                "id='" + id + '\'' +
                ", population=" + population +
                ", residents=" + residents +
                '}';
    }

    static mapping = {
        domain 'Country'
    }


}