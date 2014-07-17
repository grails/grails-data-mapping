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
    public String getLabelsWithInheritance() {
        StringBuilder sb = new StringBuilder();
        appendRecursive(sb);
        return sb.toString();
    }

    private void appendRecursive(StringBuilder sb){
        sb.append(getLabelsAsString());
        if (getParentEntity()!=null) {
            ((GraphPersistentEntity)getParentEntity()).appendRecursive(sb);
        }
    }

    public String getLabel() {
        String localLabel = (String) mappedForm.getLabels();
        if (localLabel==null) {
            localLabel = getDiscriminator();
        }
        return localLabel;
    }

    public Collection<String> getLabels() {
        Object objs = mappedForm.getLabels();
        if (objs == null) {
            return Collections.singleton(getDiscriminator());
        } else if (objs instanceof String) {
            String s = (String) objs;
            return Collections.singleton(s);
        } else if (objs instanceof Object[]) {
            Object[] labels = (Object[]) objs;

            Collection<String> retVal = new ArrayList<String>();

            for (int i = 0; i < labels.length; i++) {
                if (labels[i] instanceof String) {
                    retVal.add((String)labels[i]);
//                } else {

                } else {
                    throw new IllegalArgumentException("dunno know how to handle " + labels[i].getClass().getName() + "=" + labels[i] + " for labels mapping");
                }

            }
            return retVal;

        } else {
            throw new IllegalArgumentException("dunno know how to handle " + objs.getClass().getName() + " " + objs + " for labels mapping");
        }
    }

    public String getFirstLabel() {
        return getLabels().iterator().next();
    }

    /**
     * return all labels as string usable for cypher, concatenated by ":"
     * @return
     */
    public String getLabelsAsString() {
        StringBuilder sb = new StringBuilder();
        for (String label: getLabels()) {
            sb.append(':').append(label);
        }
        return sb.toString();
    }
}
