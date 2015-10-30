package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.neo4j.identity.SnowflakeIdGenerator
import org.grails.datastore.gorm.neo4j.mapping.config.Neo4jEntity
import org.grails.datastore.mapping.model.AbstractPersistentEntity
import org.grails.datastore.mapping.model.ClassMapping
import org.grails.datastore.mapping.model.DatastoreConfigurationException
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.util.ClassUtils


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
public class GraphPersistentEntity extends AbstractPersistentEntity<Neo4jEntity> {

    public static final String LABEL_SEPARATOR = ':'
    protected final Neo4jEntity mappedForm;
    protected final Collection<String> staticLabels = []
    protected Collection<Object> labelObjects
    protected final boolean hasDynamicLabels
    protected final boolean hasDynamicAssociations
    protected IdGenerator idGenerator

    public GraphPersistentEntity(Class javaClass, MappingContext context) {
        super(javaClass, context);
        this.mappedForm = (Neo4jEntity) context.getMappingFactory().createMappedForm(this);
        this.hasDynamicAssociations = mappedForm.isDynamicAssociations()
        this.hasDynamicLabels = establishLabels()
    }

    @Override
    void initialize() {
        super.initialize()
        def generatorType = getIdentity().getMapping().getMappedForm().getGenerator()
        this.idGenerator = createIdGenerator(generatorType)
    }

    protected IdGenerator createIdGenerator(String generatorType) {
        try {
            def type = generatorType == null ? IdGenerator.Type.SNOWFLAKE : IdGenerator.Type.valueOf(generatorType.toUpperCase())
            switch(type) {
                case IdGenerator.Type.NATIVE:
                    // for the native generator use null to indicate that generation requires an insert
                    return null
                case IdGenerator.Type.ASSIGNED:
                    throw new DatastoreConfigurationException("Assigned identifiers are currently not supported")
                case IdGenerator.Type.SNOWFLAKE:
                    return SnowflakeIdGenerator.INSTANCE
                default:
                    return SnowflakeIdGenerator.INSTANCE
            }
        } catch (IllegalArgumentException e) {
            try {
                def generatorClass = ClassUtils.forName(generatorType)
                if(IdGenerator.isAssignableFrom(generatorClass)) {
                    return (IdGenerator)generatorClass.newInstance()
                }
                else {
                    throw new DatastoreConfigurationException("Entity $javaClass defines an invalid id generator [$generatorClass]. The class must implement the IdGenerator interface")
                }
            } catch (Throwable e2) {
                def types = IdGenerator.Type.values().collect() { Enum en -> "'${en.name()}'" }.join(',')
                throw new DatastoreConfigurationException("Entity $javaClass defines an invalid id generator [$generatorType]. Should be one of ${types} or a class that implements the IdGenerator interface",e2 )
            }
        }
    }

    @Override
    public ClassMapping<Neo4jEntity> getMapping() {
        new GraphClassMapping(this, context);
    }

    /**
     * @return The ID generator to use
     */
    IdGenerator getIdGenerator() {
        return idGenerator
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

    protected boolean establishLabels() {
        labelObjects = establishLabelObjects()
        boolean hasDynamicLabels = labelObjects.any() { it instanceof Closure }
        for (obj in labelObjects) {
            String label = getLabelFor(obj)
            if (label != null) {
                staticLabels.add(label)
            }
        }
        return hasDynamicLabels
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

    boolean hasDynamicAssociations() {
        return this.hasDynamicAssociations
    }
}
