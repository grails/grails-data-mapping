package org.grails.datastore.gorm

import grails.gorm.tests.PlantNumericIdValue

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import org.grails.datastore.gorm.dynamodb.DynamoDBGormEnhancer
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.dynamodb.DynamoDBDatastore
import org.grails.datastore.mapping.dynamodb.config.DynamoDBMappingContext
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBAssociationInfo
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBTableResolver
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBTableResolverFactory
import org.grails.datastore.mapping.dynamodb.util.DynamoDBTemplate
import org.grails.datastore.mapping.dynamodb.util.DynamoDBUtil
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.context.support.GenericApplicationContext
import org.springframework.util.StringUtils
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * In order to run AWS DynamoDB tests you have to define a file called 'aws.properties' in your home dir
 * with the following properties:
 * AWS_ACCESS_KEY and AWS_SECRET_KEY
 *
 * your aws credentials and then invoke this command from main directory:
 * gradlew grails-datastore-gorm-dynamodb:test
 *
 * or this one to run one specific test:
 * gradlew -Dtest.single=CrudOperationsSpec grails-datastore-gorm-dynamodb:test
 *
 * @author graemerocher
 * @author Roman Stepanenko
 */
class Setup {

    static Set<String> tableNames = new HashSet<String>() //we keep track of each table to drop at the end because DynamoDB is expensive for a large number of tables

    static dynamoDB
    static session

    static destroy() {
//        session.nativeInterface.dropDatabase()
        List<String> existingTables = dynamoDB.getDynamoDBTemplate().listTables()
        println ''+ new Date() + ': currently existing dynamodb tables: '+existingTables.size()+" : "+existingTables
        tableNames.each { table->
            boolean delete=false
            if (delete && existingTables.contains(table)) {
                println ''+ new Date() + ': deleting dynamodb table: '+table
                dynamoDB.getDynamoDBTemplate().deleteTable(table)
            }
        }
    }

    static Session setup(classes) {
        classes.add(PlantNumericIdValue.class)

        def env = System.getenv()
        final userHome = System.getProperty("user.home")
        def settingsFile = new File(userHome, "aws.properties")
        def connectionDetails = [:]
        if (settingsFile.exists()) {
            def props = new Properties()
            settingsFile.withReader { reader ->
                props.load(reader)
            }
            connectionDetails.put(DynamoDBDatastore.ACCESS_KEY, props['AWS_ACCESS_KEY'])
            connectionDetails.put(DynamoDBDatastore.SECRET_KEY, props['AWS_SECRET_KEY'])
        }

        connectionDetails.put(DynamoDBDatastore.TABLE_NAME_PREFIX_KEY, "TEST_")
        connectionDetails.put(DynamoDBDatastore.DELAY_AFTER_WRITES_MS, "3000") //this flag will cause pausing for that many MS after each write - to fight eventual consistency

        dynamoDB = new DynamoDBDatastore(new DynamoDBMappingContext(), connectionDetails)
        def ctx = new GenericApplicationContext()
        ctx.refresh()
        dynamoDB.applicationContext = ctx
        dynamoDB.afterPropertiesSet()

        for (cls in classes) {
            dynamoDB.mappingContext.addPersistentEntity(cls)
        }

        cleanOrCreateDomainsIfNeeded(classes, dynamoDB.mappingContext, dynamoDB)

        PersistentEntity entity = dynamoDB.mappingContext.persistentEntities.find { PersistentEntity e -> e.name.contains("TestEntity")}

        dynamoDB.mappingContext.addEntityValidator(entity, [
                supports: { Class c -> true },
                validate: { Object o, Errors errors ->
                    if (!StringUtils.hasText(o.name)) {
                        errors.rejectValue("name", "name.is.blank")
                    }
                }
        ] as Validator)

        def enhancer = new DynamoDBGormEnhancer(dynamoDB, new DatastoreTransactionManager(datastore: dynamoDB))
        enhancer.enhance()

        dynamoDB.mappingContext.addMappingContextListener({ e ->
            enhancer.enhance e
        } as MappingContext.Listener)

        dynamoDB.applicationContext.addApplicationListener new DomainEventListener(dynamoDB)
        dynamoDB.applicationContext.addApplicationListener new AutoTimestampEventListener(dynamoDB)

        session = dynamoDB.connect()

        return session
    }

    /**
     * Creates AWS domain if AWS domain corresponding to a test entity class does not exist, or cleans it if it does exist.
     * @param domainClasses
     * @param mappingContext
     * @param dynamoDBDatastore
     */
    static void cleanOrCreateDomainsIfNeeded(def domainClasses, mappingContext, dynamoDBDatastore) {
        DynamoDBTemplate template = dynamoDBDatastore.getDynamoDBTemplate()
        List<String> existingTables = template.listTables()
        DynamoDBTableResolverFactory resolverFactory = new DynamoDBTableResolverFactory()

        ExecutorService executorService = Executors.newFixedThreadPool(10) //dynamodb allows no more than 10 tables to be created simultaneously
        CountDownLatch latch = new CountDownLatch(domainClasses.size())
        for (dc in domainClasses) {
            def domainClass = dc //explicitly declare local variable which we will be using from the thread
            //do dynamoDB work in parallel threads for each entity to speed things up
            executorService.execute({
                try {
                    PersistentEntity entity = mappingContext.getPersistentEntity(domainClass.getName())
                    DynamoDBTableResolver tableResolver = resolverFactory.buildResolver(entity, dynamoDBDatastore)
                    def tables = tableResolver.getAllTablesForEntity()
                    tables.each { table ->
                        tableNames.add(table)
                        clearOrCreateTable(dynamoDBDatastore, existingTables, table)
                        //create domains for associations
                        entity.getAssociations().each { association ->
                            DynamoDBAssociationInfo associationInfo = dynamoDBDatastore.getAssociationInfo(association)
                            if (associationInfo) {
                                tableNames.add(associationInfo.getTableName())
                                clearOrCreateTable(dynamoDBDatastore, existingTables, associationInfo.getTableName())
                            }
                        }
                    }
                } finally {
                    latch.countDown()
                }
            } as Runnable)
        }
        latch.await()
    }

    static clearOrCreateTable(DynamoDBDatastore datastore, def existingTables, String tableName) {
        if (existingTables.contains(tableName)) {
            datastore.getDynamoDBTemplate().deleteAllItems(tableName) //delete all items there
        } else {
            //create it
            datastore.getDynamoDBTemplate().createTable(tableName,
                    DynamoDBUtil.createIdKeySchema(),
                    DynamoDBUtil.createDefaultProvisionedThroughput(datastore)
            )
        }
    }
}
