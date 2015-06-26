package grails.test.runtime.gorm

import grails.test.mixin.gorm.Domain
import grails.test.runtime.SharedRuntimeConfigurer
import grails.test.runtime.TestRuntimeUtil
import groovy.transform.CompileStatic;

@CompileStatic
class GormTestPluginUtil {
    public static Set<Class<?>> collectDomainClassesFromAnnotations(Class annotatedClazz, Class<? extends SharedRuntimeConfigurer> sharedRuntimeConfigurerClazz = null) {
        List<Domain> allAnnotations = collectDomainAnnotations(annotatedClazz, sharedRuntimeConfigurerClazz)
        (Set<Class<?>>)allAnnotations.inject([] as Set) { Set<Class<?>> accumulator, Domain domainAnnotation ->
            accumulator.addAll(domainAnnotation.value() as List)            
            accumulator
        }
    }
    
    public static List<Domain> collectDomainAnnotations(Class annotatedClazz, Class<? extends SharedRuntimeConfigurer> sharedRuntimeConfigurerClazz) {
        List<Domain> allAnnotations = []
        appendDomainAnnotations(allAnnotations, annotatedClazz)
        appendDomainAnnotations(allAnnotations, sharedRuntimeConfigurerClazz)
        allAnnotations
    }

    private static appendDomainAnnotations(List allAnnotations, Class annotatedClazz) {
        if(annotatedClazz) {
            allAnnotations.addAll(TestRuntimeUtil.collectAllAnnotations(annotatedClazz, Domain, true) as List)
        }
    }
}
