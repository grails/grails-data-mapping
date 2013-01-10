package org.grails.datastore.gorm.dynamodb.plugin.support

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.grails.datastore.gorm.plugin.support.ApplicationContextConfigurer
import org.grails.datastore.mapping.dynamodb.DynamoDBDatastore
import org.grails.datastore.mapping.dynamodb.config.DynamoDBMappingContext
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBAssociationInfo
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBTableResolver
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBTableResolverFactory
import org.grails.datastore.mapping.dynamodb.util.DynamoDBConst
import org.grails.datastore.mapping.dynamodb.util.DynamoDBTemplate
import org.grails.datastore.mapping.dynamodb.util.DynamoDBUtil
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.springframework.context.ConfigurableApplicationContext

import com.amazonaws.services.dynamodb.model.KeySchema
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput
import com.amazonaws.services.dynamodb.model.TableDescription

class DynamoDBApplicationContextConfigurer extends ApplicationContextConfigurer {

    DynamoDBApplicationContextConfigurer() {
        super("DynamoDB")
    }

    @Override
    void configure(ConfigurableApplicationContext ctx) {
        super.configure(ctx)

        GrailsPluginManager pluginManager = ctx.pluginManager
        GrailsApplication application = ctx.grailsApplication

        def dynamoDBDomainClasses = []
        dynamoDBDomainClassProcessor(application, pluginManager, { dc ->
            dynamoDBDomainClasses.add(dc) //collect domain classes which are stored via DynamoDB
        })

        //explicitly register dynamodb domain classes with datastore
        DynamoDBDatastore dynamoDBDatastore = ctx.dynamodbDatastore
        DynamoDBMappingContext mappingContext = ctx.dynamodbMappingContext

        dynamoDBDomainClasses.each { domainClass ->
            PersistentEntity entity = mappingContext.getPersistentEntity(domainClass.clazz.getName())
            dynamoDBDatastore.persistentEntityAdded(entity)
        }

        def dynamoDBConfig = application.config?.grails?.dynamodb
        //determine dbCreate flag and create/delete AWS tables if needed
        handleDBCreate(dynamoDBConfig,
                application,
                dynamoDBDomainClasses,
                mappingContext,
                dynamoDBDatastore
        ) //similar to JDBC datastore, do 'create' or 'drop-create'
    }

    /**
     * Iterates over all domain classes which are mapped with DynamoDB and passes them to the specified closure
     */
    def dynamoDBDomainClassProcessor = { application, pluginManager, closure ->
        def isHibernateInstalled = pluginManager.hasGrailsPlugin("hibernate")
        for (dc in application.domainClasses) {
            def cls = dc.clazz
            def cpf = ClassPropertyFetcher.forClass(cls)
            def mappedWith = cpf.getStaticPropertyValue(GrailsDomainClassProperty.MAPPING_STRATEGY, String)
            if (mappedWith == DynamoDBConst.DYNAMO_DB_MAP_WITH_VALUE || (!isHibernateInstalled && mappedWith == null)) {
                closure.call(dc)
            }
        }
    }

    def handleDBCreate = { dynamoDBConfig, application, dynamoDBDomainClasses, mappingContext, dynamoDBDatastore ->
        String dbCreate = dynamoDBConfig.dbCreate
        boolean drop = false
        boolean create = false
        if ("drop-create" == dbCreate) {
            drop = true
            create = true
        } else if ("create" == dbCreate) {
            create = true
        } else if ("drop" == dbCreate) {
            drop = true
        }

        //protection against accidental drop
        boolean disableDrop = dynamoDBConfig.disableDrop
        if (disableDrop && drop) {
            throw new IllegalArgumentException("Value of disableDrop is " + disableDrop + " while dbCreate is " +
                dbCreate + ". Throwing an exception to prevent accidental drop of the data")
        }

        def numOfThreads = 10 //how many parallel threads are used to create dbCreate functionality in parallel, dynamo DB has a max of 10 concurrent threads

        Executor executor = Executors.newFixedThreadPool(numOfThreads)

        DynamoDBTemplate template = dynamoDBDatastore.getDynamoDBTemplate()
        List<String> existingTables = template.listTables()
        DynamoDBTableResolverFactory resolverFactory = new DynamoDBTableResolverFactory()
        CountDownLatch latch = new CountDownLatch(dynamoDBDomainClasses.size())

        for (dc in dynamoDBDomainClasses) {
            def domainClass = dc.clazz //explicitly declare local variable which we will be using from the thread
            //do dynamoDB work in parallel threads for each domain class to speed things up
            executor.execute({
                try {
                    PersistentEntity entity = mappingContext.getPersistentEntity(domainClass.getName())
                    KeySchema keySchema = DynamoDBUtil.getKeySchema(entity, dynamoDBDatastore)
                    ProvisionedThroughput provisionedThroughput = DynamoDBUtil.getProvisionedThroughput(entity, dynamoDBDatastore)

                    DynamoDBTableResolver tableResolver = resolverFactory.buildResolver(entity, dynamoDBDatastore)
                    def tables = tableResolver.getAllTablesForEntity()
                    tables.each { table ->
                        handleTable(dynamoDBDatastore, existingTables, table, drop, create, keySchema, provisionedThroughput)
                        //handle tables for associations
                        entity.getAssociations().each { association ->
                            DynamoDBAssociationInfo associationInfo = dynamoDBDatastore.getAssociationInfo(association)
                            if (associationInfo) {
                                handleTable(dynamoDBDatastore, existingTables, associationInfo.getTableName(), drop, create, keySchema, provisionedThroughput)
                            }
                        }
                    }
                } finally {
                    latch.countDown()
                }
            })
        }

        //if needed, drop/create hilo id generator table
        String hiloTableName = DynamoDBUtil.getPrefixedTableName(dynamoDBDatastore.getTableNamePrefix(), DynamoDBConst.ID_GENERATOR_HI_LO_TABLE_NAME)
        handleTable(dynamoDBDatastore, existingTables, hiloTableName, drop, create, DynamoDBUtil.createIdKeySchema(), DynamoDBUtil.createDefaultProvisionedThroughput(dynamoDBDatastore))

        latch.await()
        executor.shutdown()
    }

    def dropAndCreateTable(DynamoDBDatastore datastore, existingTables, tableName, KeySchema keySchema, ProvisionedThroughput throughput) {
        if (existingTables.contains(tableName)) {
            //in theory if the table already exists we just have to clear it - it is faster than dropping and re-creating it
            //however, before we decide it is okay to clear we have to compare the throughput - if it has changed we actually have to
            //re-create the table...
            TableDescription tableDescription = datastore.getDynamoDBTemplate().describeTable(tableName)
            if (tableDescription.getProvisionedThroughput().getReadCapacityUnits().equals(throughput.getReadCapacityUnits()) &&
                    tableDescription.getProvisionedThroughput().getWriteCapacityUnits().equals(throughput.getWriteCapacityUnits())) {
                //ok, just clear the data
                datastore.getDynamoDBTemplate().deleteAllItems(tableName) //delete all items there
            } else {
                //have to drop and create it
                datastore.getDynamoDBTemplate().deleteTable(tableName)
                datastore.getDynamoDBTemplate().createTable(tableName, keySchema, throughput)
            }
        } else {
            //create it
            datastore.getDynamoDBTemplate().createTable(tableName, keySchema, throughput)
        }
    }

    def createTableIfDoesNotExist(DynamoDBDatastore datastore, existingTables, tableName, KeySchema keySchema, ProvisionedThroughput throughput) {
        if (!existingTables.contains(tableName)) {
            datastore.getDynamoDBTemplate().createTable(tableName, keySchema, throughput)
        }
    }

    def deleteTableIfExists(DynamoDBDatastore datastore, existingTables, tableName) {
        if (existingTables.contains(tableName)) {
            datastore.getDynamoDBTemplate().deleteTable(tableName)
        }
    }

    def handleTable(DynamoDBDatastore datastore, existingTables, tableName, boolean drop, boolean create, KeySchema keySchema, ProvisionedThroughput throughput) {
        if (drop && create) {
            dropAndCreateTable(datastore, existingTables, tableName, keySchema, throughput)
        } else if (create) {
            createTableIfDoesNotExist(datastore, existingTables, tableName, keySchema, throughput)
        } else if (drop) {
            deleteTableIfExists(datastore, existingTables, tableName)
        }
    }
}
