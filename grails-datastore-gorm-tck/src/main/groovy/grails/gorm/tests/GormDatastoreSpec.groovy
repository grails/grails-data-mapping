package grails.gorm.tests

import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.Session

import spock.lang.*

/**
 * A Spec base class that manages a Session for each feature as well as
 * meta class cleanup on the Entity classes in the TCK.
 *
 * Users of this class need to provide a "setup" class at runtime that
 * provides the session instance. It *must* have the following name:
 *
 * - org.grails.datastore.gorm.Setup
 *
 * This class must contain a static no-arg method called "setup()"
 * that returns a Session instance.
 */
abstract class GormDatastoreSpec extends Specification {

    static final SETUP_CLASS_NAME = 'org.grails.datastore.gorm.Setup'
    static final TEST_CLASSES = [
         Task, Person, ModifyPerson, Pet, PetType, PersonEvent, Book, Highway,
         TestEntity, ChildEntity, CommonTypes, Location, City, Country, Plant,
         PlantCategory, Publication, OptLockVersioned, OptLockNotVersioned,
         ClassWithNoArgBeforeValidate, ClassWithListArgBeforeValidate, 
         ClassWithOverloadedBeforeValidate]

    @Shared Class setupClass

    Session session

    def setupSpec() {
        ExpandoMetaClass.enableGlobally()
        setupClass = loadSetupClass()
    }

    def setup() {
        cleanRegistry()
        session = setupClass.setup(TEST_CLASSES)
        DatastoreUtils.bindSession session
    }

    def cleanup() {
        session?.disconnect()
        try {
            setupClass.destroy()
        } catch(e) {
            println "ERROR: Exception during test cleanup: ${e.message}"
        }

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
