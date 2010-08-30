package grails.gorm.tests

import spock.lang.*
import org.springframework.datastore.core.Session

/**
 * A Spec base class that manages a Session for each feature as well as
 * meta class cleanup on the Entity classes in the TCK.
 * 
 * Users of this class need to provide a “setup” class at runtime that 
 * provides the session instance. It *must* have the following name:
 * 
 * - org.grails.datastore.gorm.Setup
 * 
 * This class must contain a static no-arg method called “setup()” 
 * that returns a Session instance.
 */
abstract class GormDatastoreSpec extends Specification {

    private static final SETUP_CLASS_NAME = 'org.grails.datastore.gorm.Setup'
    private static final CLASSES_TO_CLEANUP = [TestEntity, ChildEntity]
    
    @Shared Class setupClass
    
    Session session
    
    def setupSpec() {
        setupClass = loadSetupClass()
    }

    def setup() {
        cleanRegistry()
        session = setupClass.setup()
    }

    def cleanup() {
        session?.disconnect()
        cleanRegistry()
    }

    private cleanRegistry() {
        for (clazz in CLASSES_TO_CLEANUP) {
            GroovySystem.metaClassRegistry.removeMetaClass(clazz)
        }
    }
    
    static private loadSetupClass() {
        try {
            getClassLoader().loadClass(SETUP_CLASS_NAME)
        } catch (ClassNotFoundException e) {
            throw new DataStoreSetupClassMissingException()
        }
    }
    
    private static class DataStoreSetupClassMissingException extends RuntimeException {
        DataStoreSetupClassMissingException() {
            super("Datastore setup class ($SETUP_CLASS_NAME) was not found")
        }
    }
}
