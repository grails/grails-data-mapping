package grails.gorm.tests

enum TestEnum {
    V1,
    V2,
    V3;

    @Override
    String toString() {
        return "Surprise!"
    }
}
