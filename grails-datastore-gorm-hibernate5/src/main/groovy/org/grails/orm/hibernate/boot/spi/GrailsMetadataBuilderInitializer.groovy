package org.grails.orm.hibernate.boot.spi


import groovy.transform.CompileStatic
import org.hibernate.annotations.common.reflection.MetadataProviderInjector
import org.hibernate.boot.MetadataBuilder
import org.hibernate.boot.internal.MetadataBuilderImpl
import org.hibernate.boot.registry.StandardServiceRegistry
import org.hibernate.boot.spi.MetadataBuilderInitializer

@CompileStatic
class GrailsMetadataBuilderInitializer implements MetadataBuilderInitializer
{
    @Override
    void contribute(MetadataBuilder metadataBuilder, StandardServiceRegistry serviceRegistry)
    {
        def manager = new GroovyReflectionManager()
        if (metadataBuilder instanceof MetadataBuilderImpl) {
            def reflectionManager = (Object)((MetadataBuilderImpl)metadataBuilder).getMetadataBuildingOptions().getReflectionManager()
            if (reflectionManager instanceof MetadataProviderInjector) {
                manager.setMetadataProvider(((MetadataProviderInjector)reflectionManager).metadataProvider)
                metadataBuilder.applyReflectionManager(manager)
            }
        }
    }
}
