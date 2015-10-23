package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.neo4j.mapping.config.Neo4jEntity
import org.grails.datastore.mapping.config.Entity
import org.grails.datastore.mapping.model.AbstractPersistentEntity
import org.grails.datastore.mapping.model.ClassMapping
import org.grails.datastore.mapping.model.MappingContext


/**
 * Represents an entity mapped to the Neo4j graph, adding support for dynamic labelling
 *
 * @author Stefan Armbruster
 * @author Graeme Rocher
 *
 * @since 1.0
 *
 */
@CompileStatic
public class GraphPersistentEntity extends AbstractPersistentEntity<Entity> {

    public static final String LABEL_SEPARATOR = ':'
    protected Neo4jEntity mappedForm;
    protected Collection<String> staticLabels = []
    protected Collection<Object> labelObjects
    protected boolean hasDynamicLabels = false

    public GraphPersistentEntity(Class javaClass, MappingContext context) {
        super(javaClass, context);
        this.mappedForm = (Neo4jEntity) context.getMappingFactory().createMappedForm(this);
        establishLabels()
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
    /**
     * @return Returns only the statically defined labels
     */
    public Collection<String> getLabels() {
        return this.staticLabels
    }

    /**
     * Get labels specific to the given instance
     *
     * @param domainInstance The domain instance
     * @return the abels
     */
    public Collection<String> getLabels(Object domainInstance) {
        if(hasDynamicLabels) {
            Collection<String> labels = []
            for(obj in labelObjects) {
                String label = getLabelFor(obj, domainInstance)
                if(label) {
                    labels.add(label)
                }
            }
            return labels
        }
        else {
            return staticLabels
        }
    }

    /**
     * @return Return only the statically defined labels as a string usable by cypher, concatenated by ":"
     */
    public String getLabelsAsString() {
        return ":${staticLabels.join(LABEL_SEPARATOR)}"
    }

    /**
     * return all labels as string usable for cypher, concatenated by ":"
     * @return
     */
    public String getLabelsAsString(Object domainInstance) {
        if(hasDynamicLabels) {
            return ":${getLabels(domainInstance).join(LABEL_SEPARATOR)}"
        }
        else {
            return getLabelsAsString()
        }
    }

    protected void establishLabels() {
        labelObjects = establishLabelObjects()
        hasDynamicLabels = labelObjects.any() { it instanceof Closure }
        for (obj in labelObjects) {
            String label = getLabelFor(obj)
            if (label != null) {
                staticLabels.add(label)
            }
        }
    }

    protected Collection<Object> establishLabelObjects() {
        Object labels = mappedForm.getLabels();

        List objs = labels instanceof Object[] ? labels as List : [labels]

        // if labels consists solely of instance-dependent labels, add default label based on class name
        if (objs.every { (it instanceof Closure) && (it.maximumNumberOfParameters == 2) }) {
            objs << null // adding -> label defaults to discriminator
        }
        return objs
    }

    private String getLabelFor(Object obj, domainInstance = null) {
        switch (obj) {
            case null:
                return discriminator
            case CharSequence:
                return ((CharSequence)obj).toString()
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
                return result?.toString()
            default:
                return obj.toString()
        }
    }

    private void appendRecursive(StringBuilder sb, domainInstance){
        sb.append(getLabelsAsString(domainInstance));
        if (getParentEntity()!=null) {
            ((GraphPersistentEntity)getParentEntity()).appendRecursive(sb, domainInstance);
        }
    }
}
