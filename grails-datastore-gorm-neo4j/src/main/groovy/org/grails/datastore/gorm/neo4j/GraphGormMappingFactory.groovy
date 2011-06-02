package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.config.AbstractGormMappingFactory
import org.springframework.datastore.mapping.model.MappingFactory
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.model.PersistentProperty
import org.springframework.datastore.mapping.config.Property
import org.springframework.datastore.mapping.reflect.ClassPropertyFetcher
import java.beans.PropertyDescriptor
import org.springframework.datastore.mapping.annotation.Index
import org.springframework.core.annotation.AnnotationUtils
import java.lang.reflect.Field
import org.springframework.util.ReflectionUtils

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 25.04.11
 * Time: 17:39
 * To change this template use File | Settings | File Templates.
 */
class GraphGormMappingFactory extends AbstractGormMappingFactory {

    @Override
    Object createMappedForm(PersistentProperty mpp) {
        def mappedForm = super.createMappedForm(mpp)

        if (hasIndexAnnotation(mpp)) {
            mappedForm.index = true
        }

        mappedForm
    }

    /**
     * check if a given {@link PersistentProperty} has {@link Index} annotation set
     * TODO: this code is almost a duplicate from {@link org.springframework.datastore.mapping.keyvalue.mapping.config.AnnotationKeyValueMappingFactory}, consider a refactoring
     * @param mpp
     * @return
     */
    boolean hasIndexAnnotation(PersistentProperty mpp) {
        final Class javaClass = mpp.owner.javaClass
        final ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(javaClass)

        final PropertyDescriptor pd = cpf.getPropertyDescriptor(mpp.name)
        Index index = AnnotationUtils.getAnnotation(pd.readMethod, Index.class)

        if (index == null) {
            final Field field = ReflectionUtils.findField(javaClass, mpp.name)
            if (field != null) {
                ReflectionUtils.makeAccessible(field)
                index = field.getAnnotation(Index.class)
            }
        }
        index != null
    }

    @Override
    protected Class getPropertyMappedFormType() {
        Property.class
    }

    @Override
    protected Class getEntityMappedFormType() {
        null
    }

}
