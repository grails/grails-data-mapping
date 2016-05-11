package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Product
import org.bson.types.ObjectId

class TextSearchSpec extends RxGormSpec {

    void "Test simple text search"() {
        given:"Some sample data"
        Product.saveAll(
                new Product(title: "Italian Coffee"),
                new Product(title: "Arabian Coffee"),
                new Product(title: "Coffee Maker"),
                new Product(title: "Coffee Grinder"),
                new Product(title: "Coffee Cake"),
                new Product(title: "Apple Cake"),
                new Product(title: "Chocolate Cake"),
                new Product(title: "Cheese Bake"),
                new Product(title: "Bake a Cake"),
                new Product(title: "Potato Bake")
        ).toBlocking().first()

        expect:"The results are correct"
        Product.search("coffee").toList().toBlocking().first().size() == 5
        Product.search("bake coffee cake").toList().toBlocking().first().size() == 10
        Product.search("bake coffee -cake").toList().toBlocking().first().size() == 6
        Product.search('"Coffee Cake"').toList().toBlocking().first().size() == 1
        Product.searchTop("cake").toList().toBlocking().first().size() == 4
        Product.searchTop("cake",3).toList().toBlocking().first().size() == 3
        Product.countHits('coffee').toBlocking().first() == 5
    }

    @Override
    List getDomainClasses() {
        [Product]
    }
}


