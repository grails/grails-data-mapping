package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS DynamoDB, uses hilo id generator for DynamoDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
@Entity
class PlantNumericIdValue implements Serializable {
    String id
    Long version

    boolean goesInPatch
    String name

    public String toString() {
        return "PlantNumericIdValue{" +
                "id='" + id + '\'' +
                ", version=" + version +
                ", goesInPatch=" + goesInPatch +
                ", name='" + name + '\'' +
                '}';
    }

    static mapping = {
        id_generator type: 'hilo', maxLo: 5
    }
}