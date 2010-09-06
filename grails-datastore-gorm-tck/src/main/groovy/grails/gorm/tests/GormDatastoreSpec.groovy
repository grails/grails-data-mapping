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
    private static final TEST_CLASSES = [Person, Pet, PetType, PersonEvent, Book, Highway,TestEntity, ChildEntity,CommonTypes, Location, City, Country, PlantCategory, Publication]
    
    @Shared Class setupClass
    
    Session session
    
    def setupSpec() {
        setupClass = loadSetupClass()
    }

    def setup() {
        cleanRegistry()
        session = setupClass.setup(TEST_CLASSES)
    }

    def cleanup() {
        session?.disconnect()
        cleanRegistry()
    }

    private cleanRegistry() {
        for (clazz in TEST_CLASSES) {
            GroovySystem.metaClassRegistry.removeMetaClass(clazz)
        }
    }
    
    static private loadSetupClass() {
        try {
            getClassLoader().loadClass(SETUP_CLASS_NAME)
        } catch (Throwable e) {
            throw new RuntimeException("Datastore setup class ($SETUP_CLASS_NAME) was not found",e)
        }
    }
    
}
