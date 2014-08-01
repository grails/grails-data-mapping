package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.neo4j.mapping.config.Neo4jEntity
import org.grails.datastore.mapping.config.Entity
import org.grails.datastore.mapping.model.AbstractPersistentEntity
import org.grails.datastore.mapping.model.ClassMapping
import org.grails.datastore.mapping.model.MappingContext


/**
 * TODO: consider using @Memoized on most methods here after move to Groovy >= 2.2
 * @author Stefan Armbruster
 */
@CompileStatic
public class GraphPersistentEntity extends AbstractPersistentEntity<Entity> {

    protected Neo4jEntity mappedForm;

    public GraphPersistentEntity(Class javaClass, MappingContext context) {
        super(javaClass, context);
        this.mappedForm = (Neo4jEntity) context.getMappingFactory().createMappedForm(this);
    }

    @Override
    public ClassMapping getMapping() {
        new GraphClassMapping(this, context);
    }

    /**
     * recursively join all discriminators up the class hierarchy
     * @return
     */
    public String getLabelsWithInheritance(domainInstance) {
        StringBuilder sb = new StringBuilder();
        appendRecursive(sb, domainInstance);
        return sb.toString();
    }

    private void appendRecursive(StringBuilder sb, domainInstance){
        sb.append(getLabelsAsString(domainInstance));
        if (getParentEntity()!=null) {
            ((GraphPersistentEntity)getParentEntity()).appendRecursive(sb, domainInstance);
        }
    }

    public Collection<String> getLabels(domainInstance=null) {
        Object labels = mappedForm.getLabels();

        List objs = labels instanceof Object[] ? labels as List : [labels]

        // if labels consists solely of instance-dependent labels, add default label based on class name
        if ( objs.every { (it instanceof Closure) && (it.maximumNumberOfParameters==2) }) {
            objs << null // adding -> label defaults to discriminator
        }

        objs.collect { getLabelFor(it, domainInstance) }.findAll { it }  // remove nulls
    }

    private Object getLabelFor(Object obj, domainInstance) {
        switch (obj) {
            case null:
                discriminator
                break
            case String:
                obj
                break
            case Closure:
                Closure closure = (Closure)obj
                Object result = null
                switch (closure.maximumNumberOfParameters) {
                    case 1:
                        result = closure(this)
                        break
                    case 2:
                        result = domainInstance == null ? null : closure(this, domainInstance)
                        break
                    default:
                        throw new IllegalArgumentException("closure specified in labels is unsupported, it expects $closure.maximumNumberOfParameters parameters.")
                }
                return result instanceof GString ? result.toString() : result
                break
            default:
                throw new IllegalArgumentException("dunno know how to handle " + obj?.getClass().getName() + " " + obj + " for labels mapping");
        }
    }

    /**
     * return all labels as string usable for cypher, concatenated by ":"
     * @return
     */
    public String getLabelsAsString(domainInstance = null) {
        StringBuilder sb = new StringBuilder();
        for (String label: getLabels(domainInstance)) {
            sb.append(':').append(label);
        }
        return sb.toString();
    }
}
