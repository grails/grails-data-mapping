package grails.gorm.services

import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus

/**
 * A GORM service that simplifies the execution of transactions
 *
 * @author Graeme Rocher
 * @since 6.1
 */
interface TransactionService {

    /**
     * Executes the given callable within the context of a transaction with the default attributes
     *
     * @param callable The callable
     * @return The result
     */
    public <T> T withTransaction(@DelegatesTo(TransactionStatus) Closure<T> callable)

    /**
     * Executes the given callable within the context of a transaction that is automatically rolled back with the default attributes
     *
     * @param callable The callable
     * @return The result
     */
    public <T> T withRollback(@DelegatesTo(TransactionStatus) Closure<T> callable)
    /**
     * Executes the given callable within the context of a new transaction with the default attributes
     *
     * @param callable The callable
     * @return The result
     */
    public <T> T withNewTransaction(@DelegatesTo(TransactionStatus) Closure<T> callable)

    /**
     * Executes the given callable within the context of a transaction with the given definition
     *
     * @param definition The transaction definition
     * @param callable The callable The callable
     * @return The result of the callable
     */
    public <T> T withTransaction(TransactionDefinition definition, @DelegatesTo(TransactionStatus) Closure<T> callable)

    /**
     * Executes the given callable within the context of a transaction with the given definition
     *
     * @param definition The transaction definition as a map
     * @param callable The callable The callable
     * @return The result of the callable
     */
    public <T> T withTransaction(Map definition, @DelegatesTo(TransactionStatus) Closure<T> callable)

    /**
     * Executes the given callable within the context of a transaction that is automatically rolled back with the default attributes
     *
     * @param definition The transaction definition
     * @param callable The callable
     * @return The result
     */
    public <T> T withRollback(TransactionDefinition definition, @DelegatesTo(TransactionStatus) Closure<T> callable)
    /**
     * Executes the given callable within the context of a new transaction with the default attributes
     *
     * @param definition The transaction definition
     * @param callable The callable
     * @return The result
     */
    public <T> T withNewTransaction(TransactionDefinition definition, @DelegatesTo(TransactionStatus) Closure<T> callable)
}