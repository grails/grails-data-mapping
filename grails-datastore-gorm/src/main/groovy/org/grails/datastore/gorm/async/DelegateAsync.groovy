package org.grails.datastore.gorm.async
import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

import org.codehaus.groovy.transform.GroovyASTTransformationClass

/**
 * An AST transformation that takes each method in the given class and adds a delegate method that returns a {@link grails.async.Promise} and executes the method asynchronously.
 * For example given the following class:
 *
 * <pre><code>
 * class BookApi {
 *    List<Book> listBooksByTitle(String title) { }
 * }
 * </code></pre>
 *
 * If the annotation is applied to a new class:
 *
 * <pre><code>
 * @DelegateAsync(BookApi)
 * class AsyncBookApi {}
 * </code></pre>
 *
 * The resulting class is transformed into:
 *
 * <pre><code>
 * class AsyncBookApi {
 *     private BookApi $bookApi
 *     Promise<List<Book>> listBooksByTitle(String title) {
 *        (Promise<List<Book>>)Promises.createPromise {
 *            $bookApi.listBooksByTitle(title)
 *        }
 *     }
 * }
 * </code></pre>
 *
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.TYPE, ElementType.FIELD])
@GroovyASTTransformationClass("org.grails.datastore.gorm.async.DelegateAsyncTransformation")
public @interface DelegateAsync {
    Class value() default DelegateAsync
}
