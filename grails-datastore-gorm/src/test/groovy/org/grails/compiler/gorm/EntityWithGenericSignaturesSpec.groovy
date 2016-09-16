package org.grails.compiler.gorm

import spock.lang.Specification

/**
 * Created by graemerocher on 16/09/2016.
 */
class EntityWithGenericSignaturesSpec extends Specification {

    void "Test compile entity with generic signatures"() {

        when:"An entity with generic signatures is compiled"
        def gcl = new GroovyClassLoader()
        Class cls = gcl.parseClass('''
package test

import grails.gorm.annotation.Entity
import grails.gorm.dirty.checking.DirtyCheck

@Entity
class Widget<SettingType extends WidgetSetting> {
    SettingType setting

}

@Entity
abstract class WidgetSetting {
     String name
}

@Entity
class HotWidgetSetting extends WidgetSetting {
    Integer temperature
}
''')

        then:"The entity is compiled correctly"
        cls.name == 'test.Widget'
        cls.getMethod("getSetting").returnType.name == 'test.WidgetSetting'
    }
}
