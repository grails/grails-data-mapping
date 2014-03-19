package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.mapping.config.Entity;
import org.grails.datastore.mapping.model.AbstractClassMapping;
import org.grails.datastore.mapping.model.AbstractPersistentEntity;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.MappingContext;

public class GraphPersistentEntity extends AbstractPersistentEntity<Entity> {

    protected Entity mappedForm;

    public GraphPersistentEntity(Class javaClass, MappingContext context) {
        super(javaClass, context);
        this.mappedForm = context.getMappingFactory().createMappedForm(this);
    }

    @Override
    public ClassMapping getMapping() {
        return new AbstractClassMapping<Entity>(this, context) {
            @Override
            public Entity getMappedForm() {
                return GraphPersistentEntity.this.mappedForm;
            }
        };
    }

    /**
     * recursively join all discriminators up the class hierarchy
     * @return
     */
    public String getLabelsWithInheritance() {
        StringBuilder sb = new StringBuilder();
        appendRecursive(sb);
        return sb.toString();
    }

    private void appendRecursive(StringBuilder sb){
        sb.append(":").append(getDiscriminator());
        if (getParentEntity()!=null) {
            ((GraphPersistentEntity)getParentEntity()).appendRecursive(sb);
        }
    }
}
