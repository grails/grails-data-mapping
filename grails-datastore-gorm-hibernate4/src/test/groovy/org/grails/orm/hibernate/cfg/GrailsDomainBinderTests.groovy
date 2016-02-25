/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.orm.hibernate.cfg

import grails.core.DefaultGrailsApplication
import grails.core.GrailsDomainClass
import grails.util.Holders
import org.grails.core.DefaultGrailsDomainClass
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.plugins.MockGrailsPluginManager
import org.grails.validation.TestClass
import org.hibernate.cfg.ImprovedNamingStrategy
import org.hibernate.mapping.*
import org.hibernate.mapping.Table
import org.springframework.context.support.GenericApplicationContext

/**
 * @author Jason Rudolph
 * @author Sergey Nebolsin
 * @since 0.4
 */
class GrailsDomainBinderTests extends GroovyTestCase {

	private static final String CACHED_MAP = '''
@grails.persistence.Entity
class Area {
    Long id
    Long version
    Map names
    static mapping = {
        names cache: true
    }
}
'''

	private static final String ONE_TO_ONE_CLASSES_DEFINITION = '''
@grails.persistence.Entity
class Species {
    Long id
    Long version
    String name
}

@grails.persistence.Entity
class Pet {
    Long id
    Long version
    Species species
}'''

	private static final String ONE_TO_MANY_CLASSES_DEFINITION = '''
@grails.persistence.Entity
class Visit {
    Long id
    Long version
    String description
}
@grails.persistence.Entity
class Pet {
    Long id
    Long version
    Set visits
    static hasMany = [visits:Visit]
    static mapping = { visits joinTable:false, nullable:false }
}
'''

	private static final String BAG_ONE_TO_MANY_CLASSES_DEFINITION = '''
@grails.persistence.Entity
class Bagged {
    Long id
    Long version
    String description
}
class Bagger {
    Long id
    Long version
    Collection bagged
    static hasMany = [bagged: Bagged]
}'''

	private static final String MANY_TO_MANY_CLASSES_DEFINITION = '''
@grails.persistence.Entity
class Specialty {
    Long id
    Long version
    String name
    Set vets
    static hasMany = [vets:Vet]
    static belongsTo = Vet
}

@grails.persistence.Entity
class Vet {
    Long id
    Long version
    Set specialities
    static hasMany = [specialities:Specialty]
}
'''

	private static final String BAG_MANY_TO_MANY_CLASSES_DEFINITION = '''
@grails.persistence.Entity
class ManyBagged {
    Long id
    Long version
    String name
    Collection baggers
    static hasMany = [baggers: ManyBagger]
    static belongsTo = ManyBagger
}

@grails.persistence.Entity
class ManyBagger {
    Long id
    Long version
    Collection bagged
    static hasMany = [bagged: ManyBagged]
}'''

	private static final String MULTI_COLUMN_USER_TYPE_DEFINITION = '''
import org.grails.orm.hibernate.cfg.*
@grails.persistence.Entity
class Item {
    Long id
    Long version
    String name
    MyType other
    MonetaryAmount price
    static mapping = {
        name column: 's_name', sqlType: 'text'
        other type: MyUserType, sqlType: 'wrapper-characters', params:[param1: 'myParam1', param2: 'myParam2']
        price type: MonetaryAmountUserType, {
            column name: 'value'
            column name: 'currency_code', sqlType: 'text'
        }
    }
}
'''

	private static final String UNIQUE_PROPERTIES = '''
@grails.persistence.Entity
class UniquePropertiesUser {
    Long id
    Long version
    String login
    String group
    String camelCased
    String employeeID
    static constraints = {
        employeeID(unique:true)
        group(unique:'camelCased')
        login(unique:['group','camelCased'])
   }
}'''

	private static final String TABLE_PER_HIERARCHY = '''
@grails.persistence.Entity
class TablePerHierarchySuperclass {
    Long id
    Long version
    String stringProperty
    String optionalStringProperty
    ProductStatus someProductStatus
    ProductStatus someOptionalProductStatus
    static constraints = {
        optionalStringProperty nullable: true
        someOptionalProductStatus nullable: true
    }
}
enum ProductStatus {
    GOOD, BAD
}
@grails.persistence.Entity
class TablePerHierarchySubclass extends TablePerHierarchySuperclass {
    Long id
    Long version
    ProductStatus productStatus
    String productName
    Integer productCount
    ProductStatus optionalProductStatus
    String optionalProductName
    Integer optionalProductCount
    static constraints = {
        optionalProductName nullable: true
        optionalProductCount nullable: true
        optionalProductStatus nullable: true
    }
}
'''

	private static final String TABLE_PER_SUBCLASS = '''
@grails.persistence.Entity
class TablePerSubclassSuperclass {
    Long id
    Long version
    String stringProperty
    String optionalStringProperty
    ProductStatus someProductStatus
    ProductStatus someOptionalProductStatus
    static constraints = {
        optionalStringProperty nullable: true
        someOptionalProductStatus nullable: true
    }
    static mapping = {
        tablePerHierarchy false
    }
}
enum ProductStatus {
    GOOD, BAD
}
@grails.persistence.Entity
class TablePerSubclassSubclass extends TablePerSubclassSuperclass {
    Long id
    Long version
    ProductStatus productStatus
    String productName
    Integer productCount
    ProductStatus optionalProductStatus
    String optionalProductName
    Integer optionalProductCount
    static constraints = {
        optionalProductName nullable: true
        optionalProductCount nullable: true
        optionalProductStatus nullable: true
    }
}
'''

	private GroovyClassLoader cl = new GroovyClassLoader()
	private GrailsDomainBinder grailsDomainBinder = new GrailsDomainBinder()

	private Class previousNamingStrategyClass

	@Override
	protected void setUp() {
		super.setUp()
		ExpandoMetaClass.enableGlobally()
		MockGrailsPluginManager pluginManager = new MockGrailsPluginManager()
		Holders.setPluginManager(pluginManager)
		previousNamingStrategyClass = grailsDomainBinder.NAMING_STRATEGIES[Mapping.DEFAULT_DATA_SOURCE].getClass()
	}

	@Override
	protected void tearDown() {
		super.tearDown()
		grailsDomainBinder.NAMING_STRATEGIES.clear()
		grailsDomainBinder.NAMING_STRATEGIES.put(
				Mapping.DEFAULT_DATA_SOURCE, ImprovedNamingStrategy.INSTANCE)
		Holders.setPluginManager(null)
		grailsDomainBinder.configureNamingStrategy previousNamingStrategyClass
	}

	/**
	 * Test for GRAILS-4200
	 */
	void testEmbeddedComponentMapping() {
		DefaultGrailsDomainConfiguration config = getDomainConfig('''
@grails.persistence.Entity
class Widget {
    Long id
    Long version
    EmbeddedWidget ew
    static embedded = ['ew']
}
@grails.persistence.Entity
class EmbeddedWidget {
   String ew
   static mapping = {
       ew column: 'widget_name'
   }
}''')

		Table tableMapping = getTableMapping("widget", config)
		Column embeddedComponentMappedColumn = tableMapping.getColumn(new Column("widget_name"))
		assertNotNull(embeddedComponentMappedColumn)
		assertEquals("widget_name", embeddedComponentMappedColumn.name)
	}

	void testLengthProperty() {
		DefaultGrailsDomainConfiguration config = getDomainConfig('''
@grails.persistence.Entity
class Widget {
    Long id
    Long version
    String name
    String description
    static mapping = {
        name column: 's_name', sqlType: 'text', length: 42
        description column: 's_description', sqlType: 'text'
    }
}''')
		Table tableMapping = getTableMapping("widget", config)
		Column nameColumn = tableMapping.getColumn(new Column("s_name"))
		Column descriptionColumn = tableMapping.getColumn(new Column("s_description"))
		assertEquals(42, nameColumn.length)
		assertEquals(Column.DEFAULT_LENGTH, descriptionColumn.length)
	}

	void testUniqueProperty() {
		DefaultGrailsDomainConfiguration config = getDomainConfig('''
@grails.persistence.Entity
class Widget {
    Long id
    Long version
    String name
    String description
    static mapping = {
        name unique: true
    }
}''')

		Table tableMapping = getTableMapping("widget", config)
		Column nameColumn = tableMapping.getColumn(new Column("name"))
		Column descriptionColumn = tableMapping.getColumn(new Column("description"))
		assertTrue nameColumn.isUnique()
		assertFalse descriptionColumn.isUnique()
	}

	void testGroupUniqueProperty() {
		DefaultGrailsDomainConfiguration config = getDomainConfig('''

@grails.persistence.Entity
class CompositeUnique1 {
    Long id
    Long version
    String name
    String surname
    static constraints = {
        name unique:'surname'
    }
}

@grails.persistence.Entity
class CompositeUnique2 {
    Long id
    Long version
    String name
    String surname
    static constraints = {
        name unique:'surname'
    }
}
''')
		Table tableMapping = getTableMapping("composite_unique1", config)
		UniqueKey key = (UniqueKey)tableMapping.getUniqueKeyIterator().next()
		assertNotNull(key)

		Table tableMapping2 = getTableMapping("composite_unique2", config)
		UniqueKey key2 = (UniqueKey)tableMapping2.getUniqueKeyIterator().next()
		assertNotNull(key2)

		assertTrue(key.name != key2.name)
	}

	void testPrecisionProperty() {
		DefaultGrailsDomainConfiguration config = getDomainConfig('''
@grails.persistence.Entity
class Widget {
    Long id
    Long version
    Float width
    Float height
    static mapping = {
        width precision: 3
    }
}''')
		Table tableMapping = getTableMapping("widget", config)
		Column heightColumn = tableMapping.getColumn(new Column("height"))
		Column widthColumn = tableMapping.getColumn(new Column("width"))
		assertEquals(3, widthColumn.precision)
		assertEquals(Column.DEFAULT_PRECISION, heightColumn.precision)
	}

	void testScaleProperty() {
		DefaultGrailsDomainConfiguration config = getDomainConfig('''
@grails.persistence.Entity
class Widget {
    Long id
    Long version
    Float width
    Float height
    static mapping = {
        width scale: 7
    }
}''')

		Table tableMapping = getTableMapping("widget", config)
		Column heightColumn = tableMapping.getColumn(new Column("height"))
		Column widthColumn = tableMapping.getColumn(new Column("width"))
		assertEquals(7, widthColumn.scale)
		assertEquals(Column.DEFAULT_SCALE, heightColumn.scale)
	}

	void testCachedMapProperty() {
		DefaultGrailsDomainConfiguration config = getDomainConfig(CACHED_MAP)
		Table table = getTableMapping("area_names", config)
		assertEquals(255, table.getColumn(new Column("names_elt")).length)
	}

	void testColumnNullabilityWithTablePerHierarchy() {
		DefaultGrailsDomainConfiguration config = getDomainConfig(TABLE_PER_HIERARCHY)

		// with tablePerHierarchy all columns related to properties defined in subclasses must
		// be nullable to allow instances of other classes in the hierarchy to be persisted
		assertColumnNullable("table_per_hierarchy_superclass", "product_status", config)
		assertColumnNullable("table_per_hierarchy_superclass", "product_name", config)
		assertColumnNullable("table_per_hierarchy_superclass", "product_count", config)
		assertColumnNullable("table_per_hierarchy_superclass", "optional_product_status", config)
		assertColumnNullable("table_per_hierarchy_superclass", "optional_product_name", config)
		assertColumnNullable("table_per_hierarchy_superclass", "optional_product_count", config)

		// columns related to required properties in the root class should not be nullable
		assertColumnNotNullable("table_per_hierarchy_superclass", "string_property", config)
		assertColumnNotNullable("table_per_hierarchy_superclass", "some_product_status", config)

		// columns related to optional properties in the root class should be nullable
		assertColumnNullable("table_per_hierarchy_superclass", "optional_string_property", config)
		assertColumnNullable("table_per_hierarchy_superclass", "some_optional_product_status", config)
	}

	void testColumnNullabilityWithTablePerSubclass() {
		DefaultGrailsDomainConfiguration config = getDomainConfig(TABLE_PER_SUBCLASS)

		// with tablePerSubclass columns related to required properties defined in subclasses
		// should not be nullable
		assertColumnNotNullable("table_per_subclass_subclass", "product_status", config)
		assertColumnNotNullable("table_per_subclass_subclass", "product_name", config)
		assertColumnNotNullable("table_per_subclass_subclass", "product_count", config)

		// with tablePerSubclass columns related to optional properties defined in subclasses
		// should be nullable
		assertColumnNullable("table_per_subclass_subclass", "optional_product_status", config)
		assertColumnNullable("table_per_subclass_subclass", "optional_product_name", config)
		assertColumnNullable("table_per_subclass_subclass", "optional_product_count", config)

		// columns related to required properties in the root class should not be nullable
		assertColumnNotNullable("table_per_subclass_superclass", "string_property", config)
		assertColumnNotNullable("table_per_subclass_superclass", "some_product_status", config)

		// columns related to optional properties in the root class should be nullable
		assertColumnNullable("table_per_subclass_superclass", "optional_string_property", config)
		assertColumnNullable("table_per_subclass_superclass", "some_optional_product_status", config)
	}
//
//    void testUniqueConstraintGeneration() {
//        DefaultGrailsDomainConfiguration config = getDomainConfig(UNIQUE_PROPERTIES)
//        assertEquals("Tables created", 1, getTableCount(config))
//        List expectedKeyColumns1 = [new Column("camel_cased"), new Column("group"), new Column("login")]
//        List expectedKeyColumns2 = [new Column("camel_cased"), new Column("group")]
//        Table mapping = config.tableMappings.next()
//        int cnt = 0
//        boolean found1 = false, found2 = false
//        for (UniqueKey key in mapping.uniqueKeyIterator) {
//            List keyColumns = key.columns
//            if (keyColumns.equals(expectedKeyColumns1)) {
//                found1 = true
//            }
//            if (keyColumns.equals(expectedKeyColumns2)) {
//                found2 = true
//            }
//            cnt++
//        }
//        assertEquals(2, cnt)
//        assertTrue mapping.getColumn(new Column("employeeID")).isUnique()
//        assertTrue found1
//        assertTrue found2
//    }
//
//    void testInsertableHibernateMapping() {
//        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
//                cl.parseClass('''
//class TestInsertableDomain {
//    Long id
//    Long version
//    String testString1
//    String testString2
//
//    static mapping = {
//       testString1 insertable:false
//       testString2 insertable:true
//    }
//}'''))
//
//        DefaultGrailsDomainConfiguration config = getDomainConfig(cl, [domainClass.clazz])
//        Field privateDomainClasses = DefaultGrailsDomainConfiguration.getDeclaredField("domainClasses")
//        privateDomainClasses.setAccessible(true)
//
//        PersistentClass persistentClass = config.getClassMapping("TestInsertableDomain")
//
//        assertFalse persistentClass.getProperty("testString1").isInsertable()
//        assertTrue persistentClass.getProperty("testString2").isInsertable()
//    }

	void testUpdateableHibernateMapping() {
		GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
				cl.parseClass('''
@grails.persistence.Entity
class TestInsertableDomain {
    Long id
    Long version
    String testString1
    String testString2

    static mapping = {
       testString1 updateable:false
       testString2 updateable:true
    }
}'''))

		DefaultGrailsDomainConfiguration config = getDomainConfig(cl, [domainClass.clazz])

		PersistentClass persistentClass = config.getClassMapping("TestInsertableDomain")

		assertFalse persistentClass.getProperty("testString1").isUpdateable()
		assertTrue persistentClass.getProperty("testString2").isUpdateable()
	}

	void testInsertableUpdateableHibernateMapping() {
		GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
				cl.parseClass('''
@grails.persistence.Entity
class TestInsertableUpdateableDomain {
    Long id
    Long version
    String testString1
    String testString2

    static mapping = {
       testString1 insertable:false, updateable:false
       testString2 updateable:false, insertable:false
    }
}'''))

		DefaultGrailsDomainConfiguration config = getDomainConfig(cl, [domainClass.clazz])

		PersistentClass persistentClass = config.getClassMapping("TestInsertableUpdateableDomain")

		Property property1 = persistentClass.getProperty("testString1")
		assertFalse property1.isInsertable()
		assertFalse property1.isUpdateable()

		Property property2 = persistentClass.getProperty("testString2")
		assertFalse property2.isUpdateable()
		assertFalse property2.isInsertable()
	}

	void testOneToOneBindingTables() {
		assertEquals("Tables created", 2, getTableCount(getDomainConfig(ONE_TO_ONE_CLASSES_DEFINITION)))
	}

	void testOneToOneBindingFk() {
		assertForeignKey("species", "pet", getDomainConfig(ONE_TO_ONE_CLASSES_DEFINITION))
	}

	void testOneToOneBindingFkColumn() {
		assertColumnNotNullable("pet", "species_id", getDomainConfig(ONE_TO_ONE_CLASSES_DEFINITION))
	}

	void testOneToManyBindingTables() {
		assertEquals("Tables created", 2, getTableCount(getDomainConfig(ONE_TO_MANY_CLASSES_DEFINITION)))
	}

	void testOneToManyBindingFk() {
		assertForeignKey("pet", "visit", getDomainConfig(ONE_TO_MANY_CLASSES_DEFINITION))
	}

/*    void testOneToManyBindingFkColumn() {
        DefaultGrailsDomainConfiguration config = getDomainConfig(ONE_TO_MANY_CLASSES_DEFINITION)
        assertColumnNotNullable("visit", "pet_visits_id", config)
    }*/

	void testManyToManyBindingTables() {
		assertEquals("Tables created", 3, getTableCount(getDomainConfig(MANY_TO_MANY_CLASSES_DEFINITION)))
	}

	void testManyToManyBindingPk() {
		Table table = getTableMapping("vet_specialities", getDomainConfig(MANY_TO_MANY_CLASSES_DEFINITION))
		assertNotNull("VET_SPECIALTY table has a PK", table.primaryKey)
		assertEquals("VET_SPECIALTY table has a 2 column PK", 2, table.primaryKey.columns.size())
	}

	void testManyToManyBindingFk() {
		DefaultGrailsDomainConfiguration config = getDomainConfig(MANY_TO_MANY_CLASSES_DEFINITION)
		assertForeignKey("specialty", "vet_specialities", config)
		assertForeignKey("vet", "vet_specialities", config)
	}

	void testManyToManyBindingFkColumn() {
		DefaultGrailsDomainConfiguration config = getDomainConfig(MANY_TO_MANY_CLASSES_DEFINITION)
		assertColumnNotNullable("vet_specialities", "vet_id", config)
		assertColumnNotNullable("vet_specialities", "specialty_id", config)
	}

	/**
	 * Tests that single- and multi-column user type mappings work
	 * correctly. Also Checks that the "sqlType" property is honoured.
	 */
	void testUserTypeMappings() {
		DefaultGrailsDomainConfiguration config = getDomainConfig(MULTI_COLUMN_USER_TYPE_DEFINITION)
		PersistentClass persistentClass = config.getClassMapping("Item")

		// First check the "name" property and its associated column.
		Property nameProperty = persistentClass.getProperty("name")
		assertEquals(1, nameProperty.columnSpan)
		assertEquals("name", nameProperty.name)

		Column column = nameProperty.columnIterator.next()
		assertEquals("s_name", column.name)
		assertEquals("text", column.sqlType)

		// Next the "other" property.
		Property otherProperty = persistentClass.getProperty("other")
		assertEquals(1, otherProperty.columnSpan)
		assertEquals("other", otherProperty.name)

		column = otherProperty.columnIterator.next()
		assertEquals("other", column.name)
		assertEquals("wrapper-characters", column.sqlType)
		assertEquals(MyUserType.name, column.value.type.name)
		assertTrue(column.value instanceof SimpleValue)
		SimpleValue v = column.value
		assertEquals("myParam1", v.typeParameters.get("param1"))
		assertEquals("myParam2", v.typeParameters.get("param2"))

		// And now for the "price" property, which should have two columns.
		Property priceProperty = persistentClass.getProperty("price")
		assertEquals(2, priceProperty.columnSpan)
		assertEquals("price", priceProperty.name)

		Iterator<?> colIter = priceProperty.columnIterator
		column = colIter.next()
		assertEquals("value", column.name)
		assertNull("SQL type should have been 'null' for 'value' column.", column.sqlType)

		column = colIter.next()
		assertEquals("currency_code", column.name)
		assertEquals("text", column.sqlType)
	}

	void testDomainClassBinding() {
		GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
				cl.parseClass('''
@grails.persistence.Entity
class BinderTestClass {
    Long id
    Long version

    String firstName
    String lastName
    String comment
    Integer age
    boolean active = true

    static constraints = {
        firstName(nullable:true,size:4..15)
        lastName(nullable:false)
        age(nullable:true)
    }
}'''))

		DefaultGrailsDomainConfiguration config = getDomainConfig(cl, cl.loadedClasses)

		// Test database mappings
		PersistentClass persistentClass = config.getClassMapping("BinderTestClass")
		assertTrue("Property [firstName] must be optional in db mapping", persistentClass.getProperty("firstName").isOptional())
		assertFalse("Property [lastName] must be required in db mapping", persistentClass.getProperty("lastName").isOptional())
		// Property must be required by default
		assertFalse("Property [comment] must be required in db mapping", persistentClass.getProperty("comment").isOptional())

		// Test properties
		assertTrue("Property [firstName] must be optional", domainClass.getPropertyByName("firstName").isOptional())
		assertFalse("Property [lastName] must be optional", domainClass.getPropertyByName("lastName").isOptional())
		assertFalse("Property [comment] must be required", domainClass.getPropertyByName("comment").isOptional())
		assertTrue("Property [age] must be optional", domainClass.getPropertyByName("age").isOptional())
	}

	void testForeignKeyColumnBinding() {
		GrailsDomainClass oneClass = new DefaultGrailsDomainClass(
				cl.parseClass('''
@grails.persistence.Entity
class TestOneSide {
    Long id
    Long version
    String name
    String description
}'''))
		GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
				cl.parseClass('''
@grails.persistence.Entity
class TestManySide {
    Long id
    Long version
    String name
    TestOneSide testOneSide

    static mapping = {
        columns {
            testOneSide column:'EXPECTED_COLUMN_NAME'
        }
    }
}'''))

		DefaultGrailsDomainConfiguration config = getDomainConfig(cl,
				[oneClass.clazz, domainClass.clazz])

		PersistentClass persistentClass = config.getClassMapping("TestManySide")

		Column column = persistentClass.getProperty("testOneSide").columnIterator.next()
		assertEquals("EXPECTED_COLUMN_NAME", column.name)
	}

	/**
	 * @see org.grails.orm.hibernate.cfg.AbstractGrailsDomainBinder#bindStringColumnConstraints(Column, PersistentProperty)
	 */
	void testBindStringColumnConstraints() {
		// Verify that the correct length is set when a maxSize constraint is applied
		PersistentProperty constrainedProperty = getConstrainedStringProperty()
		constrainedProperty.mapping.mappedForm.maxSize = 30
		assertColumnLength(constrainedProperty, 30)

		// Verify that the correct length is set when a size constraint is applied
		constrainedProperty = getConstrainedStringProperty()
		constrainedProperty.mapping.mappedForm.size = new IntRange(6, 32768)
		assertColumnLength(constrainedProperty, 32768)

		// Verify that the default length remains intact when no size-related constraints are applied
		constrainedProperty = getConstrainedStringProperty()
		assertColumnLength(constrainedProperty, Column.DEFAULT_LENGTH)

		// Verify that the correct length is set when an inList constraint is applied
		constrainedProperty = getConstrainedStringProperty()
		constrainedProperty.mapping.mappedForm.inList = ["Groovy", "Java", "C++"]
		assertColumnLength(constrainedProperty, 6)

		// Verify that the correct length is set when a maxSize constraint *and* an inList constraint are *both* applied
		constrainedProperty = getConstrainedStringProperty()
		constrainedProperty.mapping.mappedForm.inList = ["Groovy", "Java", "C++"]
		constrainedProperty.mapping.mappedForm.maxSize = 30
		assertColumnLength(constrainedProperty, 30)
	}

	/**
	 * @see org.grails.orm.hibernate.cfg.AbstractGrailsDomainBinder#bindNumericColumnConstraints(Column, PersistentProperty, ColumnConfig)
	 */
	void testBindNumericColumnConstraints() {
		PersistentProperty constrainedProperty = getConstrainedBigDecimalProperty()
		// maxSize and minSize constraint has the number with the most digits
		constrainedProperty.mapping.mappedForm.maxSize = 123
		constrainedProperty.mapping.mappedForm.minSize = 0
		assertColumnPrecisionAndScale(constrainedProperty, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE)

		// Verify that the correct precision is set when the max constraint has the number with the most digits
		constrainedProperty = getConstrainedBigDecimalProperty()
		// maxSize and minSize constraint has the number with the most digits
		constrainedProperty.mapping.mappedForm.max = new BigDecimal("123.45")
		constrainedProperty.mapping.mappedForm.min = new BigDecimal("0")
		assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE)

		// Verify that the correct precision is set when the minSize constraint has the number with the most digits
		constrainedProperty = getConstrainedBigDecimalProperty()
		// maxSize and minSize constraint has the number with the most digits
		constrainedProperty.mapping.mappedForm.max = new BigDecimal("123.45")
		constrainedProperty.mapping.mappedForm.min = new BigDecimal("-123.45")
		assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE)

		// Verify that the correct precision is set when the high value of a floating point range constraint has the number with the most digits
		constrainedProperty = getConstrainedBigDecimalProperty()
		constrainedProperty.mapping.mappedForm.range = new ObjectRange(new BigDecimal("0"), new BigDecimal("123.45"))
		assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE)

		// Verify that the correct precision is set when the low value of a floating point range constraint has the number with the most digits
		constrainedProperty = getConstrainedBigDecimalProperty()
		constrainedProperty.mapping.mappedForm.range = new ObjectRange(new BigDecimal("-123.45"), new BigDecimal("123.45"))
		assertColumnPrecisionAndScale(constrainedProperty, 5, Column.DEFAULT_SCALE)

		// Verify that the correct scale is set when the scale constraint is specified in isolation
		constrainedProperty = getConstrainedBigDecimalProperty()
		constrainedProperty.mapping.mappedForm.scale = 4
		assertColumnPrecisionAndScale(constrainedProperty, Column.DEFAULT_PRECISION, 4)

		// Verify that the precision is set correctly for a floating point number with a min/max constraint and a scale...
		//  1) where the min/max constraint includes fewer decimal places than the scale constraint
		constrainedProperty = getConstrainedBigDecimalProperty()
		// maxSize and minSize constraint has the number with the most digits
		constrainedProperty.mapping.mappedForm.max = new BigDecimal("123.45")
		constrainedProperty.mapping.mappedForm.min = new BigDecimal("0")
		constrainedProperty.mapping.mappedForm.scale = 3
		assertColumnPrecisionAndScale(constrainedProperty, 6, 3) // precision (6) = number of integer digits in max constraint ("123.45") + scale (3)

		//  2) where the min/max constraint includes more decimal places than the scale constraint
		constrainedProperty = getConstrainedBigDecimalProperty()
		// maxSize and minSize constraint has the number with the most digits
		constrainedProperty.mapping.mappedForm.max = new BigDecimal("123.4567")
		constrainedProperty.mapping.mappedForm.min = new BigDecimal("0")
		constrainedProperty.mapping.mappedForm.scale = 3
		assertColumnPrecisionAndScale(constrainedProperty, 7, 3) // precision (7) = number of digits in max constraint ("123.4567")

		// Verify that the correct precision is set when the only one of 'min' and 'max' constraint specified
		constrainedProperty = getConstrainedBigDecimalProperty()
		constrainedProperty.mapping.mappedForm.max = new BigDecimal("123.4567")
		assertColumnPrecisionAndScale(constrainedProperty, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE)

		constrainedProperty = getConstrainedBigDecimalProperty()
		constrainedProperty.mapping.mappedForm.max = new BigDecimal("12345678901234567890.4567")
		assertColumnPrecisionAndScale(constrainedProperty, 24, Column.DEFAULT_SCALE)

		constrainedProperty = getConstrainedBigDecimalProperty()
		constrainedProperty.mapping.mappedForm.min = new BigDecimal("-123.4567")
		assertColumnPrecisionAndScale(constrainedProperty, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE)

		constrainedProperty = getConstrainedBigDecimalProperty()
		constrainedProperty.mapping.mappedForm.min = new BigDecimal("-12345678901234567890.4567")
		assertColumnPrecisionAndScale(constrainedProperty, 24, Column.DEFAULT_SCALE)
	}

	void testDefaultNamingStrategy() {

		GrailsDomainClass oneClass = new DefaultGrailsDomainClass(
				cl.parseClass('''
@grails.persistence.Entity
class TestOneSide {
    Long id
    Long version
    String fooName
    String barDescriPtion
}'''))
		GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
				cl.parseClass('''
@grails.persistence.Entity
class TestManySide {
    Long id
    Long version
    TestOneSide testOneSide

    static mapping = {
        columns {
            testOneSide column:'EXPECTED_COLUMN_NAME'
        }
    }
}'''))

		DefaultGrailsDomainConfiguration config = getDomainConfig(cl,
				[oneClass.clazz, domainClass.clazz])

		PersistentClass persistentClass = config.getClassMapping("TestOneSide")
		assertEquals("test_one_side", persistentClass.table.name)

		Column column = persistentClass.getProperty("id").columnIterator.next()
		assertEquals("id", column.name)

		column = persistentClass.getProperty("version").columnIterator.next()
		assertEquals("version", column.name)

		column = persistentClass.getProperty("fooName").columnIterator.next()
		assertEquals("foo_name", column.name)

		column = persistentClass.getProperty("barDescriPtion").columnIterator.next()
		assertEquals("bar_descri_ption", column.name)

		persistentClass = config.getClassMapping("TestManySide")
		assertEquals("test_many_side", persistentClass.table.name)

		column = persistentClass.getProperty("id").columnIterator.next()
		assertEquals("id", column.name)

		column = persistentClass.getProperty("version").columnIterator.next()
		assertEquals("version", column.name)

		column = persistentClass.getProperty("testOneSide").columnIterator.next()
		assertEquals("EXPECTED_COLUMN_NAME", column.name)
	}

//    void testTableNamePrefixing() {
//        def widgetClass = new DefaultGrailsDomainClass(
//                cl.parseClass('''
//class WidgetClass {
//    Long id
//    Long version
//}'''))
//        def personClass = new DefaultGrailsDomainClass(
//                cl.parseClass('''
//class MyPluginPersonClass {
//    Long id
//    Long version
//}'''))
//
//        def gadgetClass = new DefaultGrailsDomainClass(
//                cl.parseClass('''
//class GadgetClass {
//    Long id
//    Long version
//}'''))
//
//        def authorClass = new DefaultGrailsDomainClass(
//                cl.parseClass('''
//class AuthorClass {
//    Long id
//    Long version
//}'''))
//
//        def grailsApplication = new DefaultGrailsApplication([widgetClass.clazz, gadgetClass.clazz, personClass.clazz, authorClass.clazz] as Class[], cl)
//
//        def myPluginMap = [:]
//        myPluginMap.getName = { -> 'MyPlugin' }
//        def myPlugin = myPluginMap as GrailsPlugin
//
//        def publisherPluginMap = [:]
//        publisherPluginMap.getName = { -> 'Publisher' }
//        def publisherPlugin = publisherPluginMap as GrailsPlugin
//
//        def pluginManagerMap = [setApplicationContext: { }]
//        def myPluginDomainClassNames = ['GadgetClass', 'MyPluginPersonClass']
//        def publisherPluginDomainClassNames = ['AuthorClass']
//        pluginManagerMap.getPluginForClass = { Class clz ->
//            if (myPluginDomainClassNames.contains(clz?.name)) {
//                return myPlugin
//            }
//            if (publisherPluginDomainClassNames.contains(clz?.name)) {
//                return publisherPlugin
//            }
//            return null
//        }
//        def pluginManager = pluginManagerMap as GrailsPluginManager
//
//        def config = getDomainConfig(grailsApplication, pluginManager)
//        def persistentClass = config.getClassMapping("WidgetClass")
//        assertEquals("widget_class", persistentClass.table.name)
//        persistentClass = config.getClassMapping("GadgetClass")
//        assertEquals("gadget_class", persistentClass.table.name)
//        persistentClass = config.getClassMapping("MyPluginPersonClass")
//        assertEquals("my_plugin_person_class", persistentClass.table.name)
//        persistentClass = config.getClassMapping("AuthorClass")
//        assertEquals("author_class", persistentClass.table.name)
//
//        grailsApplication.config['grails.gorm.table.prefix.enabled'] = true
//        config = getDomainConfig(grailsApplication, pluginManager)
//        persistentClass = config.getClassMapping("WidgetClass")
//        assertEquals("widget_class", persistentClass.table.name)
//        persistentClass = config.getClassMapping("GadgetClass")
//        assertEquals("my_plugin_gadget_class", persistentClass.table.name)
//        persistentClass = config.getClassMapping("MyPluginPersonClass")
//        assertEquals("my_plugin_person_class", persistentClass.table.name)
//        persistentClass = config.getClassMapping("AuthorClass")
//        assertEquals("publisher_author_class", persistentClass.table.name)
//
//        grailsApplication.config['grails.gorm.table.prefix.enabled'] = false
//        config = getDomainConfig(grailsApplication, pluginManager)
//        persistentClass = config.getClassMapping("WidgetClass")
//        assertEquals("widget_class", persistentClass.table.name)
//        persistentClass = config.getClassMapping("GadgetClass")
//        assertEquals("gadget_class", persistentClass.table.name)
//        persistentClass = config.getClassMapping("MyPluginPersonClass")
//        assertEquals("my_plugin_person_class", persistentClass.table.name)
//        persistentClass = config.getClassMapping("AuthorClass")
//        assertEquals("author_class", persistentClass.table.name)
//
//        grailsApplication.config['grails.gorm.publisher.table.prefix.enabled'] = true
//        config = getDomainConfig(grailsApplication, pluginManager)
//        persistentClass = config.getClassMapping("WidgetClass")
//        assertEquals("widget_class", persistentClass.table.name)
//        persistentClass = config.getClassMapping("GadgetClass")
//        assertEquals("gadget_class", persistentClass.table.name)
//        persistentClass = config.getClassMapping("MyPluginPersonClass")
//        assertEquals("my_plugin_person_class", persistentClass.table.name)
//        persistentClass = config.getClassMapping("AuthorClass")
//        assertEquals("publisher_author_class", persistentClass.table.name)
//
//        grailsApplication.config['grails.gorm.table.prefix.enabled'] = true
//        grailsApplication.config['grails.gorm.myPlugin.table.prefix.enabled'] = false
//        config = getDomainConfig(grailsApplication, pluginManager)
//        persistentClass = config.getClassMapping("WidgetClass")
//        assertEquals("widget_class", persistentClass.table.name)
//        persistentClass = config.getClassMapping("GadgetClass")
//        assertEquals("gadget_class", persistentClass.table.name)
//        persistentClass = config.getClassMapping("MyPluginPersonClass")
//        assertEquals("my_plugin_person_class", persistentClass.table.name)
//        persistentClass = config.getClassMapping("AuthorClass")
//        assertEquals("publisher_author_class", persistentClass.table.name)
//    }

	void testCustomNamingStrategy() {

		// somewhat artificial in that it doesn't test that setting the property
		// in DataSource.groovy works, but that's handled in DataSourceConfigurationTests
		grailsDomainBinder.configureNamingStrategy(CustomNamingStrategy)

		GrailsDomainClass oneClass = new DefaultGrailsDomainClass(
				cl.parseClass('''
@grails.persistence.Entity
class TestOneSide {
    Long id
    Long version
    String fooName
    String barDescriPtion
}'''))
		GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
				cl.parseClass('''
@grails.persistence.Entity
class TestManySide {
    Long id
    Long version
    TestOneSide testOneSide

    static mapping = {
        columns {
            testOneSide column:'EXPECTED_COLUMN_NAME'
        }
    }
}'''))

		DefaultGrailsDomainConfiguration config = getDomainConfig(cl,
				[oneClass.clazz, domainClass.clazz])

		PersistentClass persistentClass = config.getClassMapping("TestOneSide")
		assertEquals("table_TestOneSide", persistentClass.table.name)

		Column column = persistentClass.getProperty("id").columnIterator.next()
		assertEquals("col_id", column.name)

		column = persistentClass.getProperty("version").columnIterator.next()
		assertEquals("col_version", column.name)

		column = persistentClass.getProperty("fooName").columnIterator.next()
		assertEquals("col_fooName", column.name)

		column = persistentClass.getProperty("barDescriPtion").columnIterator.next()
		assertEquals("col_barDescriPtion", column.name)

		persistentClass = config.getClassMapping("TestManySide")
		assertEquals("table_TestManySide", persistentClass.table.name)

		column = persistentClass.getProperty("id").columnIterator.next()
		assertEquals("col_id", column.name)

		column = persistentClass.getProperty("version").columnIterator.next()
		assertEquals("col_version", column.name)

		column = persistentClass.getProperty("testOneSide").columnIterator.next()
		assertEquals("EXPECTED_COLUMN_NAME", column.name)
	}

	void testCustomNamingStrategyWithCollection() {

		grailsDomainBinder.configureNamingStrategy(CustomNamingStrategy)

		GrailsDomainClass oneClass = new DefaultGrailsDomainClass(
				cl.parseClass('''
@grails.persistence.Entity
class TestOneSide2 {
    Long id
    Long version
    String fooName
    Set others
    static hasMany = [others: TestOneSide2]
}'''))
		GrailsDomainClass domainClass = new DefaultGrailsDomainClass(
				cl.parseClass('''
@grails.persistence.Entity
class TestManySide2 {
    Long id
    Long version
}'''))

		DefaultGrailsDomainConfiguration config = getDomainConfig(cl, [oneClass.clazz, domainClass.clazz])
		assertEquals '2EDIS_ENO_TSET_ELBAT_2EDIS_ENO_TSET_ELBAT', config.getCollectionMapping('TestOneSide2.others').collectionTable.name
	}

	void testCustomNamingStrategyAsInstance() {

		// somewhat artificial in that it doesn't test that setting the property
		// in DataSource.groovy works, but that's handled in DataSourceConfigurationTests
		def instance = new CustomNamingStrategy()
		grailsDomainBinder.configureNamingStrategy(instance)

		GrailsDomainClass oneClass = new DefaultGrailsDomainClass(
				cl.parseClass('''
@grails.persistence.Entity
class TestOneSide {
    Long id
    Long version
    String fooName
    String barDescriPtion
}'''))

		assert instance.is(grailsDomainBinder.getNamingStrategy('sessionFactory'))
		assert instance.is(grailsDomainBinder.NAMING_STRATEGIES.DEFAULT)

		DefaultGrailsDomainConfiguration config = getDomainConfig(cl, [oneClass.clazz])

		PersistentClass persistentClass = config.getClassMapping("TestOneSide")
		assertEquals("table_TestOneSide", persistentClass.table.name)

		Column column = persistentClass.getProperty("id").columnIterator.next()
		assertEquals("col_id", column.name)
	}

	void testManyToManyWithBag() {
		DefaultGrailsDomainConfiguration config = getDomainConfig(BAG_MANY_TO_MANY_CLASSES_DEFINITION)

		org.hibernate.mapping.Collection c = findCollection(config, 'ManyBagged.baggers')
		assertNotNull c
		assertTrue c instanceof Bag
		assertSame getTableMapping('many_bagged', config), c.table

		c = findCollection(config, 'ManyBagger.bagged')
		assertNotNull c
		assertTrue c instanceof Bag
		assertSame getTableMapping('many_bagger', config), c.table
	}

	void testOneToManyWithBag() {
		DefaultGrailsDomainConfiguration config = getDomainConfig(BAG_ONE_TO_MANY_CLASSES_DEFINITION)
		org.hibernate.mapping.Collection c = findCollection(config, 'Bagger.bagged')
		assertNotNull c
		assertTrue c instanceof Bag
		assertSame getTableMapping('bagger', config), c.table
	}

	void testEnumProperty() {
		DefaultGrailsDomainConfiguration config = getDomainConfig('''
enum AlertType {
    INFO, WARN, ERROR
}
@grails.persistence.Entity
class Alert {
    Long id
    Long version
    String message
    AlertType alertType
    static mapping = {
        alertType sqlType: 'char', length: 5
    }
}''')
		Table tableMapping = getTableMapping("alert", config)
		assertNotNull("Cannot find table mapping", tableMapping)
		Column enumColumn = tableMapping.getColumn(new Column("alert_type"))
		// we are mainly interested in length, but also check sqlType.
		assertNotNull(enumColumn)
		assertEquals(5, enumColumn.length)
		assertEquals('char', enumColumn.sqlType)
		assertEquals(Column.DEFAULT_PRECISION, enumColumn.precision)
		assertEquals(Column.DEFAULT_SCALE, enumColumn.scale)
	}

	void testTrackSaveUpdateCascade() {
		assertTrue(getCustomCascadedProperty('save-update').isExplicitSaveUpdateCascade())
	}

	void testTrackAllCascade() {
		assertTrue(getCustomCascadedProperty('all').isExplicitSaveUpdateCascade())
	}

	void testTrackAllDeleteOrphanCascade() {

		assertTrue(getCustomCascadedProperty('all-delete-orphan').isExplicitSaveUpdateCascade())
	}

	void testTrackNonSaveUpdateCascade() {
		assertTrue(!getCustomCascadedProperty('delete').isExplicitSaveUpdateCascade())
	}

	private PropertyConfig getCustomCascadedProperty(String cascadeValue) {
		def context = new HibernateMappingContext()
		def child = context.addPersistentEntity(
				cl.parseClass('''
@grails.persistence.Entity
class CascadeChild {
    Long id
    Long version
}''')
		)




		PersistentEntity cascadeParent = context.addPersistentEntity(
				cl.parseClass("""\
@grails.persistence.Entity
class CascadeParent {
    Long id
    Long version
    CascadeChild child
    static mapping = {
        child cascade: '${cascadeValue}'
    }
}"""))
		grailsDomainBinder.evaluateMapping(cascadeParent)
		return (PropertyConfig) cascadeParent.persistentProperties.find { it.name == 'child' }.mapping.mappedForm
	}

	private org.hibernate.mapping.Collection findCollection(DefaultGrailsDomainConfiguration config, String role) {
		config.collectionMappings.find { it.role == role }
	}

	private DefaultGrailsDomainConfiguration getDomainConfig(String classesDefinition) {
		cl.parseClass(classesDefinition)
		return getDomainConfig(cl, cl.loadedClasses)
	}

	private DefaultGrailsDomainConfiguration getDomainConfig(GroovyClassLoader cl, classes) {
		def grailsApplication = new DefaultGrailsApplication(classes as Class[], cl)
		def pluginManager = new MockGrailsPluginManager(grailsApplication)
		getDomainConfig grailsApplication, pluginManager
	}

	private DefaultGrailsDomainConfiguration getDomainConfig(grailsApplication, pluginManager) {
		def mainContext = new GenericApplicationContext()
		mainContext.defaultListableBeanFactory.registerSingleton 'pluginManager', pluginManager
		mainContext.refresh()
		grailsApplication.setMainContext(mainContext)
		grailsApplication.initialise()
		DefaultGrailsDomainConfiguration config = new DefaultGrailsDomainConfiguration(
				grailsApplication: grailsApplication)
		config.buildMappings()
		return config
	}

	private Table getTableMapping(String tablename, DefaultGrailsDomainConfiguration config) {
		for (Table table in config.tableMappings) {
			if (tablename.equals(table.name)) {
				return table
			}
		}
		null
	}

	private int getTableCount(DefaultGrailsDomainConfiguration config) {
		config.tableMappings.size()
	}

	private void assertForeignKey(String parentTablename, String childTablename, DefaultGrailsDomainConfiguration config) {
		Table childTable = getTableMapping(childTablename, config)
		for (ForeignKey fk in childTable.foreignKeyIterator) {
			if (parentTablename.equals(fk.referencedTable.name)) {
				return
			}
		}
		fail "FK $childTablename->$parentTablename doesn't exist"
	}

	private void assertColumnNotNullable(String tablename, String columnName, DefaultGrailsDomainConfiguration config) {
		Table table = getTableMapping(tablename, config)
		assertFalse(table.name + "." + columnName + " is nullable",
				table.getColumn(new Column(columnName)).isNullable())
	}

	private void assertColumnNullable(String tablename, String columnName, DefaultGrailsDomainConfiguration config) {
		Table table = getTableMapping(tablename, config)
		assertTrue(table.name + "." + columnName + " is not nullable",
				table.getColumn(new Column(columnName)).isNullable())
	}

	private void assertColumnLength(PersistentProperty constrainedProperty, int expectedLength) {
		Column column = new Column()
		grailsDomainBinder.bindStringColumnConstraints(column, constrainedProperty)
		assertEquals(expectedLength, column.length)
	}

	private void assertColumnPrecisionAndScale(PersistentProperty constrainedProperty, int expectedPrecision, int expectedScale) {
		Column column = new Column()
		grailsDomainBinder.bindNumericColumnConstraints(column, constrainedProperty, null)
		assertEquals(expectedPrecision, column.precision)
		assertEquals(expectedScale, column.scale)
	}

	private PersistentProperty getConstrainedBigDecimalProperty() {
		return getConstrainedProperty("testBigDecimal")
	}

	private PersistentProperty getConstrainedStringProperty() {
		return getConstrainedProperty("testString")
	}

	private PersistentProperty getConstrainedProperty(String propertyName) {
		HibernateMappingContext context = new HibernateMappingContext()
		def entity = context.addPersistentEntity(TestClass)
		entity.getPropertyByName(propertyName)
	}

	static class CustomNamingStrategy extends ImprovedNamingStrategy {
		private static final long serialVersionUID = 1

		@Override
		String classToTableName(String className) {
			"table_" + unqualify(className)
		}

		@Override
		String tableName(String tableName) {
			String name = super.tableName(tableName)
			tableName.contains('TestOneSide2') ? name.toUpperCase().reverse() : name
		}

		@Override
		String propertyToColumnName(String propertyName) {
			"col_" + unqualify(propertyName)
		}

		private String unqualify(String s) {
			int position = s.lastIndexOf('.')
			position < 0 ? s : s.substring(position + 1)
		}
	}
}
