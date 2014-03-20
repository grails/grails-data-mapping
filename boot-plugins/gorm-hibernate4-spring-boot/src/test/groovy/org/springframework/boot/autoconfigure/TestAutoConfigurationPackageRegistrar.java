package org.springframework.boot.autoconfigure;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class TestAutoConfigurationPackageRegistrar implements
        ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata,
                                        BeanDefinitionRegistry registry) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata
                .getAnnotationAttributes(TestAutoConfigurationPackage.class.getName(),
                        true));
        AutoConfigurationPackages.set(registry,
                ClassUtils.getPackageName(attributes.getString("value")));
    }

}
