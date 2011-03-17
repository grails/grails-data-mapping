package org.springframework.datastore.mapping.node.mapping.config;

import groovy.lang.Closure;

import java.util.HashMap;
import java.util.Map;

import org.springframework.datastore.mapping.config.groovy.MappingConfigurationBuilder;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.model.config.GormProperties;
import org.springframework.datastore.mapping.node.mapping.Node;
import org.springframework.datastore.mapping.node.mapping.NodeMappingFactory;
import org.springframework.datastore.mapping.node.mapping.NodeProperty;
import org.springframework.datastore.mapping.reflect.ClassPropertyFetcher;

/**
 * TODO: write javadoc
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
public class GormNodeMappingFactory extends NodeMappingFactory {

    private Map<PersistentEntity, Map> entityToPropertyMap = new HashMap<PersistentEntity, Map>();

     @Override
    public Node createMappedForm(PersistentEntity entity) {
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(entity.getJavaClass());
        final Closure value = cpf.getStaticPropertyValue(GormProperties.MAPPING, Closure.class);
        if (value != null) {
            Node node = new Node();
            MappingConfigurationBuilder builder = new MappingConfigurationBuilder(node, Node.class);
            builder.evaluate(value);
            entityToPropertyMap.put(entity, builder.getProperties());
            return node;
        }
        return super.createMappedForm(entity);
    }

    @Override
    public NodeProperty createMappedForm(PersistentProperty mpp) {
        Map properties = entityToPropertyMap.get(mpp.getOwner());
        if (properties != null && properties.containsKey(mpp.getName())) {
            return (NodeProperty) properties.get(mpp.getName());
        }
        return super.createMappedForm(mpp);
    }
}
