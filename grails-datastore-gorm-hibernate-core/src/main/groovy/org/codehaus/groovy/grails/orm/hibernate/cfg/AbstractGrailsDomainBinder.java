/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.orm.hibernate.cfg;

import grails.util.GrailsNameUtils;
import groovy.lang.Closure;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.exceptions.GrailsDomainException;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.cfg.*;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.mapping.*;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Table;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.type.*;
import org.springframework.context.ApplicationContext;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Types;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles the binding Grails domain classes and properties to the Hibernate runtime meta model.
 * Based on the HbmBinder code in Hibernate core and influenced by AnnotationsBinder.
 *
 * @author Graeme Rocher
 * @since 0.1
 */
public abstract class AbstractGrailsDomainBinder {

    protected static final String CASCADE_ALL_DELETE_ORPHAN = "all-delete-orphan";
    protected static final String FOREIGN_KEY_SUFFIX = "_id";
    protected static final String STRING_TYPE = "string";
    protected static final String EMPTY_PATH = "";
    protected static final char UNDERSCORE = '_';
    protected static final String CASCADE_ALL = "all";
    protected static final String CASCADE_SAVE_UPDATE = "save-update";
    protected static final String CASCADE_NONE = "none";
    protected static final String BACKTICK = "`";

    protected static final Map<Class<?>, Mapping> MAPPING_CACHE = new HashMap<Class<?>, Mapping>();
    protected static final String ENUM_TYPE_CLASS = "org.hibernate.type.EnumType";
    protected static final String ENUM_CLASS_PROP = "enumClass";
    protected static final String ENUM_TYPE_PROP = "type";
    protected static final String DEFAULT_ENUM_TYPE = "default";

    protected Log LOG = LogFactory.getLog(getClass());

    protected final CollectionType CT = new CollectionType(null, this) {
        public Collection create(GrailsDomainClassProperty property, PersistentClass owner, String path, Mappings mappings, String sessionFactoryBeanName) {
            return null;
        }
    };

    /**
     * Overrideable naming strategy. Defaults to <code>ImprovedNamingStrategy</code> but can
     * be configured in DataSource.groovy via <code>hibernate.naming_strategy = ...</code>.
     */
    public static Map<String, NamingStrategy> NAMING_STRATEGIES = new HashMap<String, NamingStrategy>();
    static {
       NAMING_STRATEGIES.put(GrailsDomainClassProperty.DEFAULT_DATA_SOURCE, ImprovedNamingStrategy.INSTANCE);
    }

    /**
     * Second pass class for grails relationships. This is required as all
     * persistent classes need to be loaded in the first pass and then relationships
     * established in the second pass compile
     *
     * @author Graeme
     */
    class GrailsCollectionSecondPass implements SecondPass {

        private static final long serialVersionUID = -5540526942092611348L;

        protected GrailsDomainClassProperty property;
        protected Mappings mappings;
        protected Collection collection;
        protected String sessionFactoryBeanName;

        public GrailsCollectionSecondPass(GrailsDomainClassProperty property, Mappings mappings,
                Collection coll,  String sessionFactoryBeanName) {
            this.property = property;
            this.mappings = mappings;
            this.collection = coll;
            this.sessionFactoryBeanName = sessionFactoryBeanName;
        }

        public void doSecondPass(Map<?, ?> persistentClasses, Map<?, ?> inheritedMetas) throws MappingException {
            bindCollectionSecondPass(property, mappings, persistentClasses, collection, sessionFactoryBeanName);
            createCollectionKeys();
        }

        protected void createCollectionKeys() {
            collection.createAllKeys();

            if (LOG.isDebugEnabled()) {
                String msg = "Mapped collection key: " + columns(collection.getKey());
                if (collection.isIndexed())
                    msg += ", index: " + columns(((IndexedCollection) collection).getIndex());
                if (collection.isOneToMany()) {
                    msg += ", one-to-many: "
                            + ((OneToMany) collection.getElement()).getReferencedEntityName();
                } else {
                    msg += ", element: " + columns(collection.getElement());
                }
                LOG.debug(msg);
            }
        }

        protected String columns(Value val) {
            StringBuilder columns = new StringBuilder();
            Iterator<?> iter = val.getColumnIterator();
            while (iter.hasNext()) {
                columns.append(((Selectable) iter.next()).getText());
                if (iter.hasNext()) columns.append(", ");
            }
            return columns.toString();
        }

        @SuppressWarnings("rawtypes")
        public void doSecondPass(Map persistentClasses) throws MappingException {
            bindCollectionSecondPass(property, mappings, persistentClasses, collection, sessionFactoryBeanName);
            createCollectionKeys();
        }
    }

    class ListSecondPass extends GrailsCollectionSecondPass {
        private static final long serialVersionUID = -3024674993774205193L;

        public ListSecondPass(GrailsDomainClassProperty property, Mappings mappings,
                Collection coll, String sessionFactoryBeanName) {
            super(property, mappings, coll, sessionFactoryBeanName);
        }

        @Override
        public void doSecondPass(Map<?, ?> persistentClasses, Map<?, ?> inheritedMetas) throws MappingException {
            bindListSecondPass(property, mappings, persistentClasses,
                    (org.hibernate.mapping.List) collection, sessionFactoryBeanName);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void doSecondPass(Map persistentClasses) throws MappingException {
            bindListSecondPass(property, mappings, persistentClasses,
                    (org.hibernate.mapping.List) collection, sessionFactoryBeanName);
        }
    }

    class MapSecondPass extends GrailsCollectionSecondPass {
        private static final long serialVersionUID = -3244991685626409031L;

        public MapSecondPass(GrailsDomainClassProperty property, Mappings mappings,
                Collection coll, String sessionFactoryBeanName) {
            super(property, mappings, coll, sessionFactoryBeanName);
        }

        @Override
        public void doSecondPass(Map<?, ?> persistentClasses, Map<?, ?> inheritedMetas) throws MappingException {
            bindMapSecondPass(property, mappings, persistentClasses,
                    (org.hibernate.mapping.Map)collection, sessionFactoryBeanName);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void doSecondPass(Map persistentClasses) throws MappingException {
            bindMapSecondPass(property, mappings, persistentClasses,
                    (org.hibernate.mapping.Map) collection, sessionFactoryBeanName);
        }
    }

    /**
     * Override the default naming strategy for the default datasource given a Class or a full class name.
     * @param strategy the class or name
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public void configureNamingStrategy(final Object strategy) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        configureNamingStrategy(GrailsDomainClassProperty.DEFAULT_DATA_SOURCE, strategy);
    }

    /**
     * Override the default naming strategy given a Class or a full class name,
     * or an instance of a NamingStrategy.
     *
     * @param datasourceName the datasource name
     * @param strategy  the class, name, or instance
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public void configureNamingStrategy(final String datasourceName, final Object strategy) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> namingStrategyClass = null;
        NamingStrategy namingStrategy;
        if (strategy instanceof Class<?>) {
            namingStrategyClass = (Class<?>)strategy;
        }
        else if (strategy instanceof CharSequence) {
            namingStrategyClass = Thread.currentThread().getContextClassLoader().loadClass(strategy.toString());
        }

        if (namingStrategyClass == null) {
            namingStrategy = (NamingStrategy)strategy;
        }
        else {
            namingStrategy = (NamingStrategy)namingStrategyClass.newInstance();
        }

        NAMING_STRATEGIES.put(datasourceName, namingStrategy);
    }

    protected void bindMapSecondPass(GrailsDomainClassProperty property, Mappings mappings,
              Map<?, ?> persistentClasses, org.hibernate.mapping.Map map, String sessionFactoryBeanName) {
        bindCollectionSecondPass(property, mappings, persistentClasses, map, sessionFactoryBeanName);

        SimpleValue value = new SimpleValue(mappings, map.getCollectionTable());

        bindSimpleValue(getIndexColumnType(property, STRING_TYPE), value, true,
                getIndexColumnName(property, sessionFactoryBeanName), mappings);
        PropertyConfig pc = getPropertyConfig(property);
        if (pc != null && pc.getIndexColumn() != null) {
            bindColumnConfigToColumn(getColumnForSimpleValue(value), getSingleColumnConfig(pc.getIndexColumn()));
        }

        if (!value.isTypeSpecified()) {
            throw new MappingException("map index element must specify a type: " + map.getRole());
        }
        map.setIndex(value);

        if (!property.isOneToMany() && !property.isManyToMany()) {
            SimpleValue elt = new SimpleValue(mappings, map.getCollectionTable());
            map.setElement(elt);

            String typeName = getTypeName(property,getPropertyConfig(property), getMapping(property.getDomainClass()));
            if (typeName == null) {
                if (property.isBasicCollectionType()) {
                    typeName = property.getReferencedPropertyType().getName();
                }
                else {
                    typeName = StandardBasicTypes.STRING.getName();
                }
            }
            bindSimpleValue(typeName, elt, false, getMapElementName(property, sessionFactoryBeanName), mappings);

            elt.setTypeName(typeName);

            map.setInverse(false);
        }
        else {
            map.setInverse(false);
        }
    }

    protected ColumnConfig getSingleColumnConfig(PropertyConfig propertyConfig) {
        if (propertyConfig != null) {
            List<ColumnConfig> columns = propertyConfig.getColumns();
            if (columns != null && !columns.isEmpty()) {
                return columns.get(0);
            }
        }
        return null;
    }

    protected void bindListSecondPass(GrailsDomainClassProperty property, Mappings mappings,
             Map<?, ?> persistentClasses, org.hibernate.mapping.List list, String sessionFactoryBeanName) {

        bindCollectionSecondPass(property, mappings, persistentClasses, list, sessionFactoryBeanName);

        String columnName = getIndexColumnName(property, sessionFactoryBeanName);
        final boolean isManyToMany = property.isManyToMany();

        if (isManyToMany && !property.isOwningSide()) {
            throw new MappingException("Invalid association [" + property.getDomainClass().getName() + "->" + property.getName() +
                    "]. List collection types only supported on the owning side of a many-to-many relationship.");
        }

        Table collectionTable = list.getCollectionTable();
        SimpleValue iv = new SimpleValue(mappings, collectionTable);
        bindSimpleValue("integer", iv, true, columnName, mappings);
        iv.setTypeName("integer");
        list.setIndex(iv);
        list.setBaseIndex(0);
        list.setInverse(false);

        Value v = list.getElement();
        v.createForeignKey();

        if (property.isBidirectional()) {

            String entityName;
            Value element = list.getElement();
            if (element instanceof ManyToOne) {
                ManyToOne manyToOne = (ManyToOne) element;
                entityName = manyToOne.getReferencedEntityName();
            } else {
                entityName = ((OneToMany) element).getReferencedEntityName();
            }

            PersistentClass referenced = mappings.getClass(entityName);

            Class<?> mappedClass = referenced.getMappedClass();
            Mapping m = getMapping(mappedClass);

            boolean compositeIdProperty = isCompositeIdProperty(m, property.getOtherSide());
            if (!compositeIdProperty) {
                Backref prop = new Backref();
                prop.setEntityName(property.getDomainClass().getFullName());
                prop.setName(UNDERSCORE + addUnderscore(property.getDomainClass().getShortName(), property.getName()) + "Backref");
                prop.setSelectable(false);
                prop.setUpdateable(false);
                if (isManyToMany) {
                    prop.setInsertable(false);
                }
                prop.setCollectionRole(list.getRole());
                prop.setValue(list.getKey());

                DependantValue value = (DependantValue) prop.getValue();
                if (!property.isCircular()) {
                    value.setNullable(false);
                }
                value.setUpdateable(true);
                prop.setOptional(false);

                referenced.addProperty(prop);
            }

            if ((!list.getKey().isNullable() && !list.isInverse()) || compositeIdProperty) {
                IndexBackref ib = new IndexBackref();
                ib.setName(UNDERSCORE + property.getName() + "IndexBackref");
                ib.setUpdateable(false);
                ib.setSelectable(false);
                if (isManyToMany) {
                    ib.setInsertable(false);
                }
                ib.setCollectionRole(list.getRole());
                ib.setEntityName(list.getOwner().getEntityName());
                ib.setValue(list.getIndex());
                referenced.addProperty(ib);
            }
        }
    }

    protected void bindCollectionSecondPass(GrailsDomainClassProperty property, Mappings mappings,
          Map<?, ?> persistentClasses, Collection collection, String sessionFactoryBeanName) {

        PersistentClass associatedClass = null;

        if (LOG.isDebugEnabled())
            LOG.debug("Mapping collection: "
                    + collection.getRole()
                    + " -> "
                    + collection.getCollectionTable().getName());

        PropertyConfig propConfig = getPropertyConfig(property);

        if (propConfig != null && !StringUtils.isBlank(propConfig.getSort())) {
            if (!property.isBidirectional() && property.isOneToMany()) {
                throw new GrailsDomainException("Default sort for associations ["+property.getDomainClass().getName()+"->" + property.getName() +
                        "] are not supported with unidirectional one to many relationships.");
            }
            GrailsDomainClass referenced = property.getReferencedDomainClass();
            if (referenced != null) {
                GrailsDomainClassProperty propertyToSortBy = referenced.getPropertyByName(propConfig.getSort());

                String associatedClassName = property.getReferencedDomainClass().getFullName();

                associatedClass = (PersistentClass) persistentClasses.get(associatedClassName);
                if (associatedClass != null) {
                    collection.setOrderBy(buildOrderByClause(propertyToSortBy.getName(), associatedClass, collection.getRole(),
                            propConfig.getOrder() != null ? propConfig.getOrder() : "asc"));
                }
            }
        }

        // Configure one-to-many
        if (collection.isOneToMany()) {

            GrailsDomainClass referenced = property.getReferencedDomainClass();
            Mapping m = getRootMapping(referenced);
            boolean tablePerSubclass = m != null && !m.getTablePerHierarchy();

            if (referenced != null && !referenced.isRoot() && !tablePerSubclass) {
                Mapping rootMapping = getRootMapping(referenced);
                String discriminatorColumnName = RootClass.DEFAULT_DISCRIMINATOR_COLUMN_NAME;

                if (rootMapping != null) {
                    final ColumnConfig discriminatorColumn = rootMapping.getDiscriminatorColumn();
                    if (discriminatorColumn != null) {
                        discriminatorColumnName = discriminatorColumn.getName();
                    }
                    if (rootMapping.getDiscriminatorMap().get("formula") != null) {
                        discriminatorColumnName = (String)m.getDiscriminatorMap().get("formula");
                    }
                }
                //NOTE: this will build the set for the in clause if it has sublcasses
                Set<String> discSet = buildDiscriminatorSet(referenced);
                String inclause = StringUtils.join(discSet, ',');

                collection.setWhere(discriminatorColumnName + " in (" + inclause + ")");
            }

            OneToMany oneToMany = (OneToMany) collection.getElement();
            String associatedClassName = oneToMany.getReferencedEntityName();

            associatedClass = (PersistentClass) persistentClasses.get(associatedClassName);
            // if there is no persistent class for the association throw exception
            if (associatedClass == null) {
                throw new MappingException("Association references unmapped class: " + oneToMany.getReferencedEntityName());
            }

            oneToMany.setAssociatedClass(associatedClass);
            if (shouldBindCollectionWithForeignKey(property)) {
                collection.setCollectionTable(associatedClass.getTable());
            }

            bindCollectionForPropertyConfig(collection, propConfig);
        }

        if (isSorted(property)) {
           collection.setSorted(true);
        }

        // setup the primary key references
        DependantValue key = createPrimaryKeyValue(mappings, property, collection, persistentClasses);

        // link a bidirectional relationship
        if (property.isBidirectional()) {
            GrailsDomainClassProperty otherSide = property.getOtherSide();
            if (otherSide.isManyToOne() && shouldBindCollectionWithForeignKey(property)) {
                linkBidirectionalOneToMany(collection, associatedClass, key, otherSide);
            } else if (property.isManyToMany() || Map.class.isAssignableFrom(property.getType())) {
                bindDependentKeyValue(property, key, mappings, sessionFactoryBeanName);
            }
        } else {
            if (hasJoinKeyMapping(propConfig)) {
                bindSimpleValue("long", key,false, propConfig.getJoinTable().getKey().getName(), mappings);
            } else {
                bindDependentKeyValue(property, key, mappings, sessionFactoryBeanName);
            }
        }
        collection.setKey(key);

        // get cache config
        if (propConfig != null) {
            CacheConfig cacheConfig = propConfig.getCache();
            if (cacheConfig != null) {
                collection.setCacheConcurrencyStrategy(cacheConfig.getUsage());
            }
        }

        // if we have a many-to-many
        if (property.isManyToMany() || isBidirectionalOneToManyMap(property)) {
            GrailsDomainClassProperty otherSide = property.getOtherSide();

            if (property.isBidirectional()) {
                if (LOG.isDebugEnabled())
                    LOG.debug("[GrailsDomainBinder] Mapping other side " + otherSide.getDomainClass().getName() + "." + otherSide.getName() + " -> " + collection.getCollectionTable().getName() + " as ManyToOne");
                ManyToOne element = new ManyToOne(mappings, collection.getCollectionTable());
                bindManyToMany(otherSide, element, mappings, sessionFactoryBeanName);
                collection.setElement(element);
                bindCollectionForPropertyConfig(collection, propConfig);
                if (property.isCircular()) {
                    collection.setInverse(false);
                }
            } else {
                // TODO support unidirectional many-to-many
            }
        } else if (shouldCollectionBindWithJoinColumn(property)) {
            bindCollectionWithJoinTable(property, mappings, collection, propConfig, sessionFactoryBeanName);

        } else if (isUnidirectionalOneToMany(property)) {
            // for non-inverse one-to-many, with a not-null fk, add a backref!
            // there are problems with list and map mappings and join columns relating to duplicate key constraints
            // TODO change this when HHH-1268 is resolved
            bindUnidirectionalOneToMany(property, mappings, collection);
        }
    }

    @SuppressWarnings("unchecked")
    protected String buildOrderByClause(String hqlOrderBy, PersistentClass associatedClass, String role, String defaultOrder) {
        String orderByString = null;
        if (hqlOrderBy != null) {
            List<String> properties = new ArrayList<String>();
            List<String> ordering = new ArrayList<String>();
            StringBuilder orderByBuffer = new StringBuilder();
            if (hqlOrderBy.length() == 0) {
                //order by id
                Iterator<?> it = associatedClass.getIdentifier().getColumnIterator();
                while (it.hasNext()) {
                    Selectable col = (Selectable) it.next();
                    orderByBuffer.append(col.getText()).append(" asc").append(", ");
                }
            }
            else {
                StringTokenizer st = new StringTokenizer(hqlOrderBy, " ,", false);
                String currentOrdering = defaultOrder;
                //FIXME make this code decent
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (isNonPropertyToken(token)) {
                        if (currentOrdering != null) {
                            throw new GrailsDomainException(
                                    "Error while parsing sort clause: " + hqlOrderBy
                                            + " (" + role + ")"
                            );
                        }
                        currentOrdering = token;
                    }
                    else {
                        //Add ordering of the previous
                        if (currentOrdering == null) {
                            //default ordering
                            ordering.add("asc");
                        }
                        else {
                            ordering.add(currentOrdering);
                            currentOrdering = null;
                        }
                        properties.add(token);
                    }
                }
                ordering.remove(0); //first one is the algorithm starter
                // add last one ordering
                if (currentOrdering == null) {
                    //default ordering
                    ordering.add(defaultOrder);
                }
                else {
                    ordering.add(currentOrdering);
                    currentOrdering = null;
                }
                int index = 0;

                for (String property : properties) {
                    Property p = BinderHelper.findPropertyByName(associatedClass, property);
                    if (p == null) {
                        throw new GrailsDomainException(
                                "property from sort clause not found: "
                                        + associatedClass.getEntityName() + "." + property
                        );
                    }
                    PersistentClass pc = p.getPersistentClass();
                    String table;
                    if (pc == null) {
                        table = "";
                    }

                    else if (pc == associatedClass
                            || (associatedClass instanceof SingleTableSubclass &&
                           pc.getMappedClass().isAssignableFrom(associatedClass.getMappedClass()))) {
                        table = "";
                    } else {
                        table = pc.getTable().getQuotedName() + ".";
                    }

                    Iterator<?> propertyColumns = p.getColumnIterator();
                    while (propertyColumns.hasNext()) {
                        Selectable column = (Selectable) propertyColumns.next();
                        orderByBuffer.append(table)
                                .append(column.getText())
                                .append(" ")
                                .append(ordering.get(index))
                                .append(", ");
                    }
                    index++;
                }
            }
            orderByString = orderByBuffer.substring(0, orderByBuffer.length() - 2);
        }
        return orderByString;
    }

    protected boolean isNonPropertyToken(String token) {
        if (" ".equals(token)) return true;
        if (",".equals(token)) return true;
        if (token.equalsIgnoreCase("desc")) return true;
        if (token.equalsIgnoreCase("asc")) return true;
        return false;
    }

    protected Set<String> buildDiscriminatorSet(GrailsDomainClass domainClass) {
        Set<String> theSet = new HashSet<String>();

        Mapping mapping = getMapping(domainClass);
        String discriminator = domainClass.getFullName();
        if (mapping != null && mapping.getDiscriminator() != null) {
            discriminator = mapping.getDiscriminator();
        }
        Mapping rootMapping = getRootMapping(domainClass);
        String quote = "'";
        if (rootMapping != null && rootMapping.getDiscriminatorMap() != null &&
            rootMapping.getDiscriminatorMap().get("type") != null && rootMapping.getDiscriminatorMap().get("type") != "string") {
            quote = "";
        }
        theSet.add(quote + discriminator + quote);

        for (GrailsDomainClass subClass : domainClass.getSubClasses()) {
            theSet.addAll(buildDiscriminatorSet(subClass));
        }
        return theSet;
    }

    protected Mapping getRootMapping(GrailsDomainClass referenced) {
        if (referenced == null) return null;
        Class<?> current = referenced.getClazz();
        while (true) {
            Class<?> superClass = current.getSuperclass();
            if (Object.class.equals(superClass)) break;
            current = superClass;
        }

        return getMapping(current);
    }

    protected boolean isBidirectionalOneToManyMap(GrailsDomainClassProperty property) {
        return Map.class.isAssignableFrom(property.getType()) && property.isBidirectional();
    }

    protected void bindCollectionWithJoinTable(GrailsDomainClassProperty property,
              Mappings mappings, Collection collection, PropertyConfig config, String sessionFactoryBeanName) {

        NamingStrategy namingStrategy = getNamingStrategy(sessionFactoryBeanName);

        SimpleValue element;
        if (property.isBasicCollectionType()) {
           element = new SimpleValue(mappings, collection.getCollectionTable());
        }
        else {
            // for a normal unidirectional one-to-many we use a join column
            element = new ManyToOne(mappings, collection.getCollectionTable());
            bindUnidirectionalOneToManyInverseValues(property, (ManyToOne) element);
        }
        collection.setInverse(false);

        String columnName;

        final boolean hasJoinColumnMapping = hasJoinColumnMapping(config);
        if (property.isBasicCollectionType()) {
            final Class<?> referencedType = property.getReferencedPropertyType();
            String className = referencedType.getName();
            final boolean isEnum = referencedType.isEnum();
            if (hasJoinColumnMapping) {
                columnName = config.getJoinTable().getColumn().getName();
            }
            else {
                columnName = isEnum ? namingStrategy.propertyToColumnName(className) :
                    addUnderscore(namingStrategy.propertyToColumnName(property.getName()),
                                  namingStrategy.propertyToColumnName(className));
            }

            if (isEnum) {
                bindEnumType(property, referencedType,element,columnName);
            }
            else {

                String typeName = getTypeName(property, config, getMapping(property.getDomainClass()));
                if (typeName == null) {
                    Type type = mappings.getTypeResolver().basic(className);
                    if (type != null) {
                        typeName = type.getName();
                    }
                }
                if (typeName == null) {
                    String domainName = property.getDomainClass().getName();
                    throw new MappingException("Missing type or column for column["+columnName+"] on domain["+domainName+"] referencing["+className+"]");
                }

                bindSimpleValue(typeName, element,true, columnName, mappings);
                if (hasJoinColumnMapping) {
                    bindColumnConfigToColumn(getColumnForSimpleValue(element), config.getJoinTable().getColumn());
                }
            }
        } else {
            final GrailsDomainClass domainClass = property.getReferencedDomainClass();

            Mapping m = getMapping(domainClass.getClazz());
            if (hasCompositeIdentifier(m)) {
                CompositeIdentity ci = (CompositeIdentity) m.getIdentity();
                bindCompositeIdentifierToManyToOne(property, element, ci, domainClass,
                        EMPTY_PATH, sessionFactoryBeanName);
            }
            else {
                if (hasJoinColumnMapping) {
                    columnName = config.getJoinTable().getColumn().getName();
                }
                else {
                    columnName = namingStrategy.propertyToColumnName(domainClass.getPropertyName()) + FOREIGN_KEY_SUFFIX;
                }

                bindSimpleValue("long", element,true, columnName, mappings);
            }
        }

        collection.setElement(element);

        bindCollectionForPropertyConfig(collection, config);
    }

    protected String addUnderscore(String s1, String s2) {
        return removeBackticks(s1) + UNDERSCORE + removeBackticks(s2);
    }

    protected String removeBackticks(String s) {
        return s.startsWith("`") && s.endsWith("`") ? s.substring(1, s.length() - 1) : s;
    }

    protected Column getColumnForSimpleValue(SimpleValue element) {
        return (Column)element.getColumnIterator().next();
    }

    protected String getTypeName(GrailsDomainClassProperty property, PropertyConfig config, Mapping mapping) {
        if (config != null && config.getType() != null) {
            final Object typeObj = config.getType();
            if (typeObj instanceof Class<?>) {
                return ((Class<?>)typeObj).getName();
            }
            return typeObj.toString();
        }

        if (mapping != null) {
            return mapping.getTypeName(property.getType());
        }

        return null;
    }

    protected void bindColumnConfigToColumn(Column column, ColumnConfig columnConfig) {
        if (columnConfig == null) {
            return;
        }

        if (columnConfig.getLength() != -1) {
            column.setLength(columnConfig.getLength());
        }
        if (columnConfig.getPrecision() != -1) {
            column.setPrecision(columnConfig.getPrecision());
        }
        if (columnConfig.getScale() != -1) {
            column.setScale(columnConfig.getScale());
        }
        if (columnConfig.getSqlType() != null && !columnConfig.getSqlType().isEmpty()) {
            column.setSqlType(columnConfig.getSqlType());
        }
        column.setUnique(columnConfig.getUnique());
    }

    protected boolean hasJoinColumnMapping(PropertyConfig config) {
        return config != null && config.getJoinTable() != null && config.getJoinTable().getColumn() != null;
    }

    protected boolean shouldCollectionBindWithJoinColumn(GrailsDomainClassProperty property) {
        PropertyConfig config = getPropertyConfig(property);
        JoinTable jt = config != null ? config.getJoinTable() : new JoinTable();

        return (isUnidirectionalOneToMany(property) || property.isBasicCollectionType()) && jt != null;
    }

    /**
     * @param property
     * @param manyToOne
     */
    protected void bindUnidirectionalOneToManyInverseValues(GrailsDomainClassProperty property, ManyToOne manyToOne) {
        PropertyConfig config = getPropertyConfig(property);
        if (config != null) {
            manyToOne.setLazy(config.getLazy());
            manyToOne.setIgnoreNotFound(config.getIgnoreNotFound());
        } else {
            manyToOne.setLazy(true);
        }

        // set referenced entity
        manyToOne.setReferencedEntityName(property.getReferencedPropertyType().getName());
    }

    protected void bindCollectionForPropertyConfig(Collection collection, PropertyConfig config) {
        if (config == null) {
            collection.setLazy(true);
            collection.setExtraLazy(false);
        } else {
            collection.setLazy(config.getLazy());
        }
    }

    public PropertyConfig getPropertyConfig(GrailsDomainClassProperty property) {
        Mapping m = getMapping(property.getDomainClass().getClazz());
        PropertyConfig config = m != null ? m.getPropertyConfig(property.getName()) : null;
        return config;
    }

    /**
     * Checks whether a property is a unidirectional non-circular one-to-many
     *
     * @param property The property to check
     * @return true if it is unidirectional and a one-to-many
     */
    protected boolean isUnidirectionalOneToMany(GrailsDomainClassProperty property) {
        return property.isOneToMany() && !property.isBidirectional();
    }

    /**
     * Binds the primary key value column
     *
     * @param property The property
     * @param key      The key
     * @param mappings The mappings
     */
    protected void bindDependentKeyValue(GrailsDomainClassProperty property, DependantValue key,
            Mappings mappings, String sessionFactoryBeanName) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("[GrailsDomainBinder] binding  [" + property.getName() + "] with dependant key");
        }

        GrailsDomainClass refDomainClass = property.getDomainClass();
        final Mapping mapping = getMapping(refDomainClass.getClazz());
        if ((shouldCollectionBindWithJoinColumn(property) && hasCompositeIdentifier(mapping)) ||
                (hasCompositeIdentifier(mapping) && property.isManyToMany())) {
            CompositeIdentity ci = (CompositeIdentity) mapping.getIdentity();
            bindCompositeIdentifierToManyToOne(property, key, ci, refDomainClass, EMPTY_PATH, sessionFactoryBeanName);
        }
        else {
            bindSimpleValue(property, null, key, EMPTY_PATH, mappings, sessionFactoryBeanName);
        }
    }

    /**
     * Creates the DependentValue object that forms a primary key reference for the collection.
     *
     * @param mappings
     * @param property          The grails property
     * @param collection        The collection object
     * @param persistentClasses
     * @return The DependantValue (key)
     */
    protected DependantValue createPrimaryKeyValue(Mappings mappings, GrailsDomainClassProperty property,
                                                        Collection collection, Map<?, ?> persistentClasses) {
        KeyValue keyValue;
        DependantValue key;
        String propertyRef = collection.getReferencedPropertyName();
        // this is to support mapping by a property
        if (propertyRef == null) {
            keyValue = collection.getOwner().getIdentifier();
        } else {
            keyValue = (KeyValue) collection.getOwner().getProperty(propertyRef).getValue();
        }

        if (LOG.isDebugEnabled())
            LOG.debug("[GrailsDomainBinder] creating dependant key value  to table [" + keyValue.getTable().getName() + "]");

        key = new DependantValue(mappings, collection.getCollectionTable(), keyValue);

        key.setTypeName(null);
        // make nullable and non-updateable
        key.setNullable(true);
        key.setUpdateable(false);
        return key;
    }

    /**
     * Binds a unidirectional one-to-many creating a psuedo back reference property in the process.
     *
     * @param property
     * @param mappings
     * @param collection
     */
    protected void bindUnidirectionalOneToMany(GrailsDomainClassProperty property, Mappings mappings, Collection collection) {
        Value v = collection.getElement();
        v.createForeignKey();
        String entityName;
        if (v instanceof ManyToOne) {
            ManyToOne manyToOne = (ManyToOne) v;

            entityName = manyToOne.getReferencedEntityName();
        } else {
            entityName = ((OneToMany) v).getReferencedEntityName();
        }
        collection.setInverse(false);
        PersistentClass referenced = mappings.getClass(entityName);
        Backref prop = new Backref();
        prop.setEntityName(property.getDomainClass().getFullName());
        prop.setName(UNDERSCORE + addUnderscore(property.getDomainClass().getShortName(), property.getName()) + "Backref");
        prop.setUpdateable(false);
        prop.setInsertable(true);
        prop.setCollectionRole(collection.getRole());
        prop.setValue(collection.getKey());
        prop.setOptional(true);

        referenced.addProperty(prop);
    }

    protected Property getProperty(PersistentClass associatedClass, String propertyName) throws MappingException {
        try {
            return associatedClass.getProperty(propertyName);
        }
        catch (MappingException e) {
            //maybe it's squirreled away in a composite primary key
            if (associatedClass.getKey() instanceof Component) {
                return ((Component) associatedClass.getKey()).getProperty(propertyName);
            }
            throw e;
        }
    }

    /**
     * Links a bidirectional one-to-many, configuring the inverse side and using a column copy to perform the link
     *
     * @param collection      The collection one-to-many
     * @param associatedClass The associated class
     * @param key             The key
     * @param otherSide       The other side of the relationship
     */
    protected void linkBidirectionalOneToMany(Collection collection, PersistentClass associatedClass, DependantValue key, GrailsDomainClassProperty otherSide) {
        collection.setInverse(true);

        // Iterator mappedByColumns = associatedClass.getProperty(otherSide.getName()).getValue().getColumnIterator();
        Iterator<?> mappedByColumns = getProperty(associatedClass, otherSide.getName()).getValue().getColumnIterator();
        while (mappedByColumns.hasNext()) {
            Column column = (Column) mappedByColumns.next();
            linkValueUsingAColumnCopy(otherSide, column, key);
        }
    }

    /**
     * Establish whether a collection property is sorted
     *
     * @param property The property
     * @return true if sorted
     */
    protected boolean isSorted(GrailsDomainClassProperty property) {
        return SortedSet.class.isAssignableFrom(property.getType());
    }

    /**
     * Binds a many-to-many relationship. A many-to-many consists of
     * - a key (a DependentValue)
     * - an element
     * <p/>
     * The element is a ManyToOne from the association table to the target entity
     *
     * @param property The grails property
     * @param element  The ManyToOne element
     * @param mappings The mappings
     */
    protected void bindManyToMany(GrailsDomainClassProperty property, ManyToOne element,
            Mappings mappings, String sessionFactoryBeanName) {
        bindManyToOne(property, element, EMPTY_PATH, mappings, sessionFactoryBeanName);
        element.setReferencedEntityName(property.getDomainClass().getFullName());
    }

    protected void linkValueUsingAColumnCopy(GrailsDomainClassProperty prop, Column column, DependantValue key) {
        Column mappingColumn = new Column();
        mappingColumn.setName(column.getName());
        mappingColumn.setLength(column.getLength());
        mappingColumn.setNullable(prop.isOptional());
        mappingColumn.setSqlType(column.getSqlType());

        mappingColumn.setValue(key);
        key.addColumn(mappingColumn);
        key.getTable().addColumn(mappingColumn);
    }

    /**
     * First pass to bind collection to Hibernate metamodel, sets up second pass
     *
     * @param property   The GrailsDomainClassProperty instance
     * @param collection The collection
     * @param owner      The owning persistent class
     * @param mappings   The Hibernate mappings instance
     * @param path
     */
    protected void bindCollection(GrailsDomainClassProperty property, Collection collection,
            PersistentClass owner, Mappings mappings, String path, String sessionFactoryBeanName) {

        // set role
        String propertyName = getNameForPropertyAndPath(property, path);
        collection.setRole(qualify(property.getDomainClass().getFullName(), propertyName));

        PropertyConfig pc = getPropertyConfig(property);
        // configure eager fetching
        if (property.getFetchMode() == GrailsDomainClassProperty.FETCH_EAGER) {
            collection.setFetchMode(FetchMode.JOIN);
        }
        else if (pc != null && pc.getFetch() != null) {
            collection.setFetchMode(pc.getFetch());
        }
        else {
            collection.setFetchMode(FetchMode.DEFAULT);
        }

        if (pc != null && pc.getCascade() != null) {
            collection.setOrphanDelete(pc.getCascade().equals(CASCADE_ALL_DELETE_ORPHAN));
        }
        // if it's a one-to-many mapping
        if (shouldBindCollectionWithForeignKey(property)) {
            OneToMany oneToMany = new OneToMany(mappings, collection.getOwner());
            collection.setElement(oneToMany);
            bindOneToMany(property, oneToMany, mappings);
        } else {
            bindCollectionTable(property, mappings, collection, owner.getTable(), sessionFactoryBeanName);

            if (!property.isOwningSide()) {
                collection.setInverse(true);
            }
        }

        if (pc != null && pc.getBatchSize() != null) {
            collection.setBatchSize(pc.getBatchSize().intValue());
        }

        // set up second pass
        if (collection instanceof org.hibernate.mapping.Set) {
            mappings.addSecondPass(new GrailsCollectionSecondPass(property, mappings, collection, sessionFactoryBeanName));
        }
        else if (collection instanceof org.hibernate.mapping.List) {
            mappings.addSecondPass(new ListSecondPass(property, mappings, collection, sessionFactoryBeanName));
        }
        else if (collection instanceof org.hibernate.mapping.Map) {
            mappings.addSecondPass(new MapSecondPass(property, mappings, collection, sessionFactoryBeanName));
        }
        else { // Collection -> Bag
            mappings.addSecondPass(new GrailsCollectionSecondPass(property, mappings, collection, sessionFactoryBeanName));
        }
    }

    /*
     * We bind collections with foreign keys if specified in the mapping and only if
     * it is a unidirectional one-to-many that is.
     */
    protected boolean shouldBindCollectionWithForeignKey(GrailsDomainClassProperty property) {
        return (property.isOneToMany() && property.isBidirectional() ||
                !shouldCollectionBindWithJoinColumn(property)) && !Map.class.isAssignableFrom(property.getType()) && !property.isManyToMany() && !property.isBasicCollectionType();
    }

    protected String getNameForPropertyAndPath(GrailsDomainClassProperty property, String path) {
        if (isNotEmpty(path)) {
            return qualify(path, property.getName());
        }
        return property.getName();
    }

    protected void bindCollectionTable(GrailsDomainClassProperty property, Mappings mappings,
            Collection collection, Table ownerTable, String sessionFactoryBeanName) {

        String owningTableSchema = ownerTable.getSchema();
        PropertyConfig config = getPropertyConfig(property);
        JoinTable jt = config != null ? config.getJoinTable() : null;

        NamingStrategy namingStrategy = getNamingStrategy(sessionFactoryBeanName);
        String tableName = (jt != null && jt.getName() != null ? jt.getName() : namingStrategy.tableName(calculateTableForMany(property, sessionFactoryBeanName)));
        String schemaName = mappings.getSchemaName();
        String catalogName = mappings.getCatalogName();
        if(jt != null) {
            if(jt.getSchema() != null) {
                schemaName = jt.getSchema();
            }
            if(jt.getCatalog() != null) {
                catalogName = jt.getCatalog();
            }
        }

        if(schemaName == null && owningTableSchema != null) {
            schemaName = owningTableSchema;
        }

        collection.setCollectionTable(mappings.addTable(
                schemaName, catalogName,
                tableName, null, false));
    }

    /**
     * Calculates the mapping table for a many-to-many. One side of
     * the relationship has to "own" the relationship so that there is not a situation
     * where you have two mapping tables for left_right and right_left
     */
    protected String calculateTableForMany(GrailsDomainClassProperty property, String sessionFactoryBeanName) {
        NamingStrategy namingStrategy = getNamingStrategy(sessionFactoryBeanName);

        String propertyColumnName = namingStrategy.propertyToColumnName(property.getName());
        //fix for GRAILS-5895
        PropertyConfig config = getPropertyConfig(property);
        JoinTable jt = config != null ? config.getJoinTable() : null;
        boolean hasJoinTableMapping = jt != null && jt.getName() != null;
        String left = getTableName(property.getDomainClass(), sessionFactoryBeanName);

        if (Map.class.isAssignableFrom(property.getType())) {
            if (hasJoinTableMapping) {
                return jt.getName();
            }
            return addUnderscore(left, propertyColumnName);
        }

        if (property.isBasicCollectionType()) {
            if (hasJoinTableMapping) {
                return jt.getName();
            }
            return addUnderscore(left, propertyColumnName);
        }

        String right = getTableName(property.getReferencedDomainClass(), sessionFactoryBeanName);

        if (property.isManyToMany()) {
            if (hasJoinTableMapping) {
                return jt.getName();
            }
            if (property.isOwningSide()) {
                return addUnderscore(left, propertyColumnName);
            }
            return addUnderscore(right, namingStrategy.propertyToColumnName(property.getOtherSide().getName()));
        }

        if (shouldCollectionBindWithJoinColumn(property)) {
            if (hasJoinTableMapping) {
                return jt.getName();
            }
            left = trimBackTigs(left);
            right = trimBackTigs(right);
            return addUnderscore(left, right);
        }

        if (property.isOwningSide()) {
            return addUnderscore(left, right);
        }
        return addUnderscore(right, left);
    }

    protected String trimBackTigs(String tableName) {
        if (tableName.startsWith(BACKTICK)) {
            return tableName.substring(1, tableName.length() - 1);
        }
        return tableName;
    }

    /**
     * Evaluates the table name for the given property
     *
     * @param domainClass The domain class to evaluate
     * @return The table name
     */
    protected String getTableName(GrailsDomainClass domainClass, String sessionFactoryBeanName) {
        Mapping m = getMapping(domainClass.getClazz());
        String tableName = null;
        if (m != null && m.getTableName() != null) {
            tableName = m.getTableName();
        }
        if (tableName == null) {
            String shortName = domainClass.getShortName();
            final GrailsApplication grailsApplication = domainClass.getGrailsApplication();
            if (grailsApplication != null) {
                final ApplicationContext mainContext = grailsApplication.getMainContext();
                if (mainContext != null && mainContext.containsBean("pluginManager")) {
                    final GrailsPluginManager pluginManager = (GrailsPluginManager) mainContext.getBean("pluginManager");
                    final GrailsPlugin pluginForClass = pluginManager.getPluginForClass(domainClass.getClazz());
                    if (pluginForClass != null) {
                        final String pluginName = pluginForClass.getName();
                        boolean shouldApplyPluginPrefix = false;
                        if (!shortName.toLowerCase().startsWith(pluginName.toLowerCase())) {
                            final String pluginSpecificConfigProperty = "grails.gorm." + GrailsNameUtils.getPropertyName(pluginName) + ".table.prefix.enabled";
                            final Map<String, Object> flatConfig = grailsApplication.getFlatConfig();
                            if (flatConfig.containsKey(pluginSpecificConfigProperty)) {
                                shouldApplyPluginPrefix = Boolean.TRUE.equals(flatConfig.get(pluginSpecificConfigProperty));
                            } else {
                                shouldApplyPluginPrefix = Boolean.TRUE.equals(flatConfig.get("grails.gorm.table.prefix.enabled"));
                            }
                        }
                        if (shouldApplyPluginPrefix) {
                            shortName = pluginName + shortName;
                        }
                    }
                }
            }
            tableName = getNamingStrategy(sessionFactoryBeanName).classToTableName(shortName);
        }
        return tableName;
    }

    protected NamingStrategy getNamingStrategy(String sessionFactoryBeanName) {
        String key = "sessionFactory".equals(sessionFactoryBeanName) ?
                GrailsDomainClassProperty.DEFAULT_DATA_SOURCE :
                    sessionFactoryBeanName.substring("sessionFactory_".length());
        return NAMING_STRATEGIES.get(key);
    }

    /**
     * Binds a Grails domain class to the Hibernate runtime meta model
     *
     * @param domainClass The domain class to bind
     * @param mappings    The existing mappings
     * @param sessionFactoryBeanName  the session factory bean name
     * @throws MappingException Thrown if the domain class uses inheritance which is not supported
     */
    public void bindClass(GrailsDomainClass domainClass, Mappings mappings, String sessionFactoryBeanName)
            throws MappingException {
        //if (domainClass.getClazz().getSuperclass() == Object.class) {
        if (domainClass.isRoot()) {
            bindRoot(domainClass, mappings, sessionFactoryBeanName);
        }
    }

    /**
     * Evaluates a Mapping object from the domain class if it has a mapping closure
     *
     * @param domainClass The domain class
     * @return the mapping
     */
    public Mapping evaluateMapping(GrailsDomainClass domainClass) {
        return evaluateMapping(domainClass, null);
    }

    public Mapping evaluateMapping(GrailsDomainClass domainClass, Closure<?> defaultMapping) {
        return evaluateMapping(domainClass, defaultMapping, true);
    }

    public Mapping evaluateMapping(GrailsDomainClass domainClass, Closure<?> defaultMapping, boolean cache) {
       try {
            Object o = GrailsClassUtils.getStaticPropertyValue(domainClass.getClazz(), GrailsDomainClassProperty.MAPPING);
            if (o != null || defaultMapping != null) {
                HibernateMappingBuilder builder = new HibernateMappingBuilder(domainClass.getFullName());
                GrailsApplication application = domainClass.getGrailsApplication();
                ApplicationContext ctx = null;
                if (application != null) {
                    ctx = application.getMainContext();
                    if (ctx == null) ctx = application.getParentContext();
                }

                Mapping m = null;
                if (defaultMapping != null) {
                    m = builder.evaluate(defaultMapping,ctx);
                }

                if (o instanceof Closure) {
                    m = builder.evaluate((Closure<?>) o,ctx);
                }

                final Object identity = m.getIdentity();
                if (identity instanceof Identity) {
                    final Identity identityObject = (Identity) identity;
                    final String idName = identityObject.getName();
                    if (idName != null && !GrailsDomainClassProperty.IDENTITY.equals(idName)) {
                        GrailsDomainClassProperty persistentProperty = domainClass.getPersistentProperty(idName);
                        if (!persistentProperty.isIdentity()) {
                            if (persistentProperty instanceof DefaultGrailsDomainClassProperty) {
                                ((DefaultGrailsDomainClassProperty)persistentProperty).setIdentity(true); // fixed for 2.2
                            }
                        }
                    }
                }

                trackCustomCascadingSaves(m, domainClass.getPersistentProperties());

                if (cache) {
                    MAPPING_CACHE.put(domainClass.getClazz(), m);
                }
                return m;
            }
            return null;
        } catch (Exception e) {
            throw new GrailsDomainException("Error evaluating ORM mappings block for domain [" +
                    domainClass.getFullName() + "]:  " + e.getMessage(), e);
        }
    }

    /**
     * Checks for any custom cascading saves set up via the mapping DSL and records them within the persistent property.
     * @param mapping The Mapping.
     * @param persistentProperties The persistent properties of the domain class.
     */
    protected void trackCustomCascadingSaves(Mapping mapping, GrailsDomainClassProperty[] persistentProperties) {
        for (GrailsDomainClassProperty property : persistentProperties) {
            PropertyConfig propConf = mapping.getPropertyConfig(property.getName());

            if (propConf != null && propConf.getCascade() != null) {
                property.setExplicitSaveUpdateCascade(isSaveUpdateCascade(propConf.getCascade()));
            }
        }
    }

    /**
     * Check if a save-update cascade is defined within the Hibernate cascade properties string.
     * @param cascade The string containing the cascade properties.
     * @return True if save-update or any other cascade property that encompasses those is present.
     */
    protected boolean isSaveUpdateCascade(String cascade) {
        String[] cascades = cascade.split(",");

        for (String cascadeProp : cascades) {
            String trimmedProp = cascadeProp.trim();

            if (CASCADE_SAVE_UPDATE.equals(trimmedProp) || CASCADE_ALL.equals(trimmedProp) || CASCADE_ALL_DELETE_ORPHAN.equals(trimmedProp)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Obtains a mapping object for the given domain class nam
     *
     * @param theClass The domain class in question
     * @return A Mapping object or null
     */
    public Mapping getMapping(Class<?> theClass) {
        return theClass == null ? null : MAPPING_CACHE.get(theClass);
    }

    /**
     * Obtains a mapping object for the given domain class nam
     *
     * @param domainClass The domain class in question
     * @return A Mapping object or null
     */
    public Mapping getMapping(GrailsDomainClass domainClass) {
        return domainClass == null ? null : MAPPING_CACHE.get(domainClass.getClazz());
    }

    public void clearMappingCache() {
        MAPPING_CACHE.clear();
    }

    public void clearMappingCache(Class<?> theClass) {
        String className = theClass.getName();
        for(Iterator<Map.Entry<Class<?>, Mapping>> it = MAPPING_CACHE.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Class<?>, Mapping> entry = it.next();
            if (className.equals(entry.getKey().getName())) {
                it.remove();
            }
        }
    }

    /**
     * Binds the specified persistant class to the runtime model based on the
     * properties defined in the domain class
     *
     * @param domainClass     The Grails domain class
     * @param persistentClass The persistant class
     * @param mappings        Existing mappings
     */
    protected void bindClass(GrailsDomainClass domainClass, PersistentClass persistentClass, Mappings mappings) {

        // set lazy loading for now
        persistentClass.setLazy(true);
        persistentClass.setEntityName(domainClass.getFullName());
        persistentClass.setJpaEntityName(unqualify(domainClass.getFullName()));
        persistentClass.setProxyInterfaceName(domainClass.getFullName());
        persistentClass.setClassName(domainClass.getFullName());

        // set dynamic insert to false
        persistentClass.setDynamicInsert(false);
        // set dynamic update to false
        persistentClass.setDynamicUpdate(false);
        // set select before update to false
        persistentClass.setSelectBeforeUpdate(false);

        // add import to mappings
        if (mappings.isAutoImport() && persistentClass.getEntityName().indexOf('.') > 0) {
            mappings.addImport(persistentClass.getEntityName(), unqualify(persistentClass.getEntityName()));
        }
    }

    /**
     * Binds a root class (one with no super classes) to the runtime meta model
     * based on the supplied Grails domain class
     *
     * @param domainClass The Grails domain class
     * @param mappings    The Hibernate Mappings object
     * @param sessionFactoryBeanName  the session factory bean name
     */
    public void bindRoot(GrailsDomainClass domainClass, Mappings mappings, String sessionFactoryBeanName) {
        if (mappings.getClass(domainClass.getFullName()) != null) {
            LOG.info("[GrailsDomainBinder] Class [" + domainClass.getFullName() + "] is already mapped, skipping.. ");
            return;
        }

        RootClass root = new RootClass();
        root.setAbstract(Modifier.isAbstract(domainClass.getClazz().getModifiers()));
        if (!domainClass.hasSubClasses()) {
            root.setPolymorphic(false);
        }
        bindClass(domainClass, root, mappings);

        Mapping m = getMapping(domainClass);

        bindRootPersistentClassCommonValues(domainClass, root, mappings, sessionFactoryBeanName);

        if (!domainClass.getSubClasses().isEmpty()) {
            boolean tablePerSubclass = m != null && !m.getTablePerHierarchy();
            if (!tablePerSubclass) {
                // if the root class has children create a discriminator property
                bindDiscriminatorProperty(root.getTable(), root, mappings);
            }
            // bind the sub classes
            bindSubClasses(domainClass, root, mappings, sessionFactoryBeanName);
        }

        if (root.getEntityPersisterClass() == null) {
            root.setEntityPersisterClass(getGroovyAwareSingleTableEntityPersisterClass());
        }
        mappings.addClass(root);
    }

    /**
     * Binds the sub classes of a root class using table-per-heirarchy inheritance mapping
     *
     * @param domainClass The root domain class to bind
     * @param parent      The parent class instance
     * @param mappings    The mappings instance
     * @param sessionFactoryBeanName  the session factory bean name
     */
    protected void bindSubClasses(GrailsDomainClass domainClass, PersistentClass parent,
            Mappings mappings, String sessionFactoryBeanName) {
        Set<GrailsDomainClass> subClasses = new HashSet<GrailsDomainClass>(domainClass.getSubClasses());

        for (GrailsDomainClass sub : subClasses) {
            if (sub.getClazz().getSuperclass().equals(domainClass.getClazz())) {
                bindSubClass(sub, parent, mappings, sessionFactoryBeanName);
            }
        }
    }

    /**
     * Binds a sub class.
     *
     * @param sub      The sub domain class instance
     * @param parent   The parent persistent class instance
     * @param mappings The mappings instance
     * @param sessionFactoryBeanName  the session factory bean name
     */
    protected void bindSubClass(GrailsDomainClass sub, PersistentClass parent,
            Mappings mappings, String sessionFactoryBeanName) {
        evaluateMapping(sub);
        Mapping m = getMapping(parent.getMappedClass());
        Subclass subClass;
        boolean tablePerSubclass = m != null && !m.getTablePerHierarchy() && !m.isTablePerConcreteClass();
        boolean tablePerConcreteClass = m != null && m.isTablePerConcreteClass();
        if (tablePerSubclass) {
            subClass = new JoinedSubclass(parent);
        }
        else if(tablePerConcreteClass) {
            subClass = new UnionSubclass(parent);
        }
        else {
            subClass = new SingleTableSubclass(parent);
            // set the descriminator value as the name of the class. This is the
            // value used by Hibernate to decide what the type of the class is
            // to perform polymorphic queries
            Mapping subMapping = getMapping(sub);
            subClass.setDiscriminatorValue(subMapping != null && subMapping.getDiscriminator() != null ? subMapping.getDiscriminator() : sub.getFullName());
            if (subMapping != null) {
                configureDerivedProperties(sub, subMapping);
            }
        }

        subClass.setEntityName(sub.getFullName());
        subClass.setJpaEntityName(unqualify(sub.getFullName()));

        parent.addSubclass(subClass);
        mappings.addClass(subClass);

        if (tablePerSubclass) {
            bindJoinedSubClass(sub, (JoinedSubclass) subClass, mappings, m, sessionFactoryBeanName);
        }
        else if( tablePerConcreteClass) {
            bindUnionSubclass(sub, (UnionSubclass) subClass, mappings, sessionFactoryBeanName);
        }
        else {
            bindSubClass(sub, subClass, mappings, sessionFactoryBeanName);
        }

        if (!sub.getSubClasses().isEmpty()) {
            // bind the sub classes
            bindSubClasses(sub, subClass, mappings, sessionFactoryBeanName);
        }
    }


    public void bindUnionSubclass(GrailsDomainClass subClass, UnionSubclass unionSubclass,
                                         Mappings mappings, String sessionFactoryBeanName) throws MappingException {


        Mapping subMapping = getMapping(subClass.getClazz());

        if ( unionSubclass.getEntityPersisterClass() == null ) {
            unionSubclass.getRootClass().setEntityPersisterClass(
                    UnionSubclassEntityPersister.class );
        }

        String schema = subMapping != null && subMapping.getTable().getSchema() != null ?
                subMapping.getTable().getSchema() : null;

        String catalog = subMapping != null && subMapping.getTable().getCatalog() != null ?
                subMapping.getTable().getCatalog() : null;

        Table denormalizedSuperTable = unionSubclass.getSuperclass().getTable();
        Table mytable = mappings.addDenormalizedTable(
                schema,
                catalog,
                getTableName(subClass, sessionFactoryBeanName),
                unionSubclass.isAbstract() != null && unionSubclass.isAbstract(),
                null,
                denormalizedSuperTable
        );
        unionSubclass.setTable( mytable );
        unionSubclass.setClassName(subClass.getFullName());

        LOG.info(
                "Mapping union-subclass: " + unionSubclass.getEntityName() +
                        " -> " + unionSubclass.getTable().getName()
        );

        createClassProperties(subClass, unionSubclass, mappings, sessionFactoryBeanName);

    }
    /**
     * Binds a joined sub-class mapping using table-per-subclass
     *
     * @param sub            The Grails sub class
     * @param joinedSubclass The Hibernate Subclass object
     * @param mappings       The mappings Object
     * @param gormMapping    The GORM mapping object
     * @param sessionFactoryBeanName  the session factory bean name
     */
    protected void bindJoinedSubClass(GrailsDomainClass sub, JoinedSubclass joinedSubclass,
            Mappings mappings, Mapping gormMapping, String sessionFactoryBeanName) {
        bindClass(sub, joinedSubclass, mappings);

        if (joinedSubclass.getEntityPersisterClass() == null) {
            joinedSubclass.getRootClass().setEntityPersisterClass(getGroovyAwareJoinedSubclassEntityPersisterClass());
        }

        Table mytable = mappings.addTable(
                mappings.getSchemaName(), mappings.getCatalogName(),
                getJoinedSubClassTableName(sub, joinedSubclass, null, mappings, sessionFactoryBeanName),
                null, false);

        joinedSubclass.setTable(mytable);
        LOG.info("Mapping joined-subclass: " + joinedSubclass.getEntityName() +
                        " -> " + joinedSubclass.getTable().getName());

        SimpleValue key = new DependantValue(mappings, mytable, joinedSubclass.getIdentifier());
        joinedSubclass.setKey(key);
        GrailsDomainClassProperty identifier = sub.getIdentifier();
        String columnName = getColumnNameForPropertyAndPath(identifier, EMPTY_PATH, null, sessionFactoryBeanName);
        bindSimpleValue(identifier.getType().getName(), key, false, columnName, mappings);

        joinedSubclass.createPrimaryKey();

        // properties
        createClassProperties(sub, joinedSubclass, mappings, sessionFactoryBeanName);
    }

    protected String getJoinedSubClassTableName(
            GrailsDomainClass sub, PersistentClass model, Table denormalizedSuperTable,
            Mappings mappings, String sessionFactoryBeanName) {

        String logicalTableName = unqualify(model.getEntityName());
        String physicalTableName = getTableName(sub, sessionFactoryBeanName);

        mappings.addTableBinding(mappings.getSchemaName(), mappings.getCatalogName(), logicalTableName, physicalTableName, denormalizedSuperTable);
        return physicalTableName;
    }

    /**
     * Binds a sub-class using table-per-hierarchy inheritance mapping
     *
     * @param sub      The Grails domain class instance representing the sub-class
     * @param subClass The Hibernate SubClass instance
     * @param mappings The mappings instance
     */
    protected void bindSubClass(GrailsDomainClass sub, Subclass subClass, Mappings mappings,
            String sessionFactoryBeanName) {
        bindClass(sub, subClass, mappings);

        if (subClass.getEntityPersisterClass() == null) {
            subClass.getRootClass().setEntityPersisterClass(getGroovyAwareSingleTableEntityPersisterClass());
        }

        if (LOG.isDebugEnabled())
            LOG.debug("Mapping subclass: " + subClass.getEntityName() +
                            " -> " + subClass.getTable().getName());

        // properties
        createClassProperties(sub, subClass, mappings, sessionFactoryBeanName);
    }

    /**
     * Creates and binds the discriminator property used in table-per-hierarchy inheritance to
     * discriminate between sub class instances
     *
     * @param table    The table to bind onto
     * @param entity   The root class entity
     * @param mappings The mappings instance
     */
    protected void bindDiscriminatorProperty(Table table, RootClass entity, Mappings mappings) {
        Mapping m = getMapping(entity.getMappedClass());
        SimpleValue d = new SimpleValue(mappings, table);
        entity.setDiscriminator(d);
        entity.setDiscriminatorValue(m != null && m.getDiscriminator() != null ? m.getDiscriminator() : entity.getClassName());

        if (m != null && m.getDiscriminatorMap().get("insert") != null) {
            entity.setDiscriminatorInsertable((Boolean)m.getDiscriminatorMap().get("insert"));
        }
        if (m != null && m.getDiscriminatorMap().get("type") != null) {
            d.setTypeName((String)m.getDiscriminatorMap().get("type"));
        }

        if (m != null && m.getDiscriminatorMap().get("formula") != null) {
            Formula formula = new Formula();
            formula.setFormula((String)m.getDiscriminatorMap().get("formula"));
            d.addFormula(formula);
        }
        else{
            bindSimpleValue(STRING_TYPE, d, false, RootClass.DEFAULT_DISCRIMINATOR_COLUMN_NAME, mappings);

            ColumnConfig cc = m == null ? null : m.getDiscriminatorColumn();
            if (cc != null) {
                Column c = (Column) d.getColumnIterator().next();
                if (cc.getName() != null) {
                    c.setName(cc.getName());
                }
                bindColumnConfigToColumn(c, cc);
            }
        }

        entity.setPolymorphic(true);
    }

    protected void configureDerivedProperties(GrailsDomainClass domainClass, Mapping m) {
        for (GrailsDomainClassProperty prop : domainClass.getPersistentProperties()) {
            PropertyConfig propertyConfig = m.getPropertyConfig(prop.getName());
            if (propertyConfig != null && propertyConfig.getFormula() != null) {
                prop.setDerived(true);
            }
        }
    }

    /*
     * Binds a persistent classes to the table representation and binds the class properties
     */
    protected void bindRootPersistentClassCommonValues(GrailsDomainClass domainClass,
            RootClass root, Mappings mappings, String sessionFactoryBeanName) {

        // get the schema and catalog names from the configuration
        Mapping m = getMapping(domainClass.getClazz());
        String schema = mappings.getSchemaName();
        String catalog = mappings.getCatalogName();

        if (m != null) {
            configureDerivedProperties(domainClass, m);
            CacheConfig cc = m.getCache();
            if (cc != null && cc.getEnabled()) {
                root.setCacheConcurrencyStrategy(cc.getUsage());
                if ("read-only".equals(cc.getUsage())) {
                    root.setMutable(false);
                }
                root.setLazyPropertiesCacheable(!"non-lazy".equals(cc.getInclude()));
            }

            Integer bs = m.getBatchSize();
            if (bs != null) {
                root.setBatchSize(bs);
            }

            if (m.getDynamicUpdate()) {
                root.setDynamicUpdate(true);
            }
            if (m.getDynamicInsert()) {
                root.setDynamicInsert(true);
            }
        }

        final boolean hasTableDefinition = m != null && m.getTable() != null;
        if (hasTableDefinition && m.getTable().getSchema() != null) {
             schema =  m.getTable().getSchema();
        }
        if (hasTableDefinition && m.getTable().getCatalog() != null) {
            catalog = m.getTable().getCatalog();
        }

        final boolean isAbstract = m != null && !m.getTablePerHierarchy() && m.isTablePerConcreteClass() && root.isAbstract();
        // create the table
        Table table = mappings.addTable(schema, catalog,
                getTableName(domainClass, sessionFactoryBeanName),
                null, isAbstract);
        root.setTable(table);

        if (LOG.isDebugEnabled()) {
            LOG.debug("[GrailsDomainBinder] Mapping Grails domain class: " + domainClass.getFullName() + " -> " + root.getTable().getName());
        }

        bindIdentity(domainClass, root, mappings, m, sessionFactoryBeanName);

        if (m == null) {
            bindVersion(domainClass.getVersion(), root, mappings, sessionFactoryBeanName);
        }
        else {
            if (m.getVersioned()) {
                bindVersion(domainClass.getVersion(), root, mappings, sessionFactoryBeanName);
            }
        }

        root.createPrimaryKey();

        createClassProperties(domainClass, root, mappings, sessionFactoryBeanName);
    }

    protected void bindIdentity(GrailsDomainClass domainClass, RootClass root, Mappings mappings,
            Mapping gormMapping, String sessionFactoryBeanName) {

        GrailsDomainClassProperty identifierProp = domainClass.getIdentifier();
        if (gormMapping == null) {
            bindSimpleId(identifierProp, root, mappings, null, sessionFactoryBeanName);
            return;
        }

        Object id = gormMapping.getIdentity();
        if (id instanceof CompositeIdentity) {
            bindCompositeId(domainClass, root, (CompositeIdentity) id, mappings, sessionFactoryBeanName);
        } else {
            final Identity identity = (Identity) id;
            String propertyName = identity.getName();
            if (propertyName != null) {
                GrailsDomainClassProperty namedIdentityProp = domainClass.getPropertyByName(propertyName);
                if (namedIdentityProp == null) {
                    throw new MappingException("Mapping specifies an identifier property name that doesn't exist ["+propertyName+"]");
                }
                if (!namedIdentityProp.equals(identifierProp)) {
                    identifierProp = namedIdentityProp;
                }
            }
            bindSimpleId(identifierProp, root, mappings, identity, sessionFactoryBeanName);
        }
    }

    protected void bindCompositeId(GrailsDomainClass domainClass, RootClass root,
            CompositeIdentity compositeIdentity, Mappings mappings, String sessionFactoryBeanName) {
        Component id = new Component(mappings, root);
        id.setNullValue("undefined");
        root.setIdentifier(id);
        root.setEmbeddedIdentifier(true);
        id.setComponentClassName(domainClass.getFullName());
        id.setKey(true);
        id.setEmbedded(true);

        String path = qualify(root.getEntityName(), "id");

        id.setRoleName(path);

        String[] props = compositeIdentity.getPropertyNames();
        for (String propName : props) {
            GrailsDomainClassProperty property = domainClass.getPropertyByName(propName);
            if (property == null) {
                throw new MappingException("Property [" + propName +
                        "] referenced in composite-id mapping of class [" + domainClass.getFullName() +
                        "] is not a valid property!");
            }

            bindComponentProperty(id, null, property, root, "", root.getTable(), mappings, sessionFactoryBeanName);
        }
    }

    /**
     * Creates and binds the properties for the specified Grails domain class and PersistentClass
     * and binds them to the Hibernate runtime meta model
     *
     * @param domainClass     The Grails domain class
     * @param persistentClass The Hibernate PersistentClass instance
     * @param mappings        The Hibernate Mappings instance
     * @param sessionFactoryBeanName  the session factory bean name
     */
    protected void createClassProperties(GrailsDomainClass domainClass, PersistentClass persistentClass,
            Mappings mappings, String sessionFactoryBeanName) {

        GrailsDomainClassProperty[] persistentProperties = domainClass.getPersistentProperties();
        Table table = persistentClass.getTable();

        Mapping gormMapping = getMapping(domainClass.getClazz());

        if (gormMapping != null) {
            table.setComment(gormMapping.getComment());
        }

        for (GrailsDomainClassProperty currentGrailsProp : persistentProperties) {

            // if its inherited skip
            boolean isBidirectionalManyToOne = isBidirectionalManyToOne(currentGrailsProp);
            if (currentGrailsProp.isInherited()) {
                continue;
            }
            if (currentGrailsProp.isInherited() && isBidirectionalManyToOne && currentGrailsProp.isCircular()) {
                continue;
            }
            if (isCompositeIdProperty(gormMapping, currentGrailsProp)) continue;
            if (isIdentityProperty(gormMapping, currentGrailsProp)) continue;

            if (LOG.isDebugEnabled()) {
                LOG.debug("[GrailsDomainBinder] Binding persistent property [" + currentGrailsProp.getName() + "]");
            }

            Value value = null;

            // see if it's a collection type
            CollectionType collectionType = CT.collectionTypeForClass(currentGrailsProp.getType());

            Class<?> userType = getUserType(currentGrailsProp);

            if (userType != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as SimpleValue");
                }
                value = new SimpleValue(mappings, table);
                bindSimpleValue(currentGrailsProp, null, (SimpleValue) value, EMPTY_PATH, mappings, sessionFactoryBeanName);
            }
            else if (collectionType != null) {
                String typeName = getTypeName(currentGrailsProp, getPropertyConfig(currentGrailsProp),gormMapping);
                if ("serializable".equals(typeName)) {
                    value = new SimpleValue(mappings, table);
                    bindSimpleValue(typeName, (SimpleValue) value, currentGrailsProp.isOptional(),
                            getColumnNameForPropertyAndPath(currentGrailsProp, EMPTY_PATH, null, sessionFactoryBeanName), mappings);
                }
                else {
                    // create collection
                    Collection collection = collectionType.create(currentGrailsProp, persistentClass,
                            EMPTY_PATH, mappings, sessionFactoryBeanName);
                    mappings.addCollection(collection);
                    value = collection;
                }
            }
            else if (currentGrailsProp.isEnum()) {
                value = new SimpleValue(mappings, table);
                bindEnumType(currentGrailsProp, (SimpleValue) value, EMPTY_PATH, sessionFactoryBeanName);
            }
            // work out what type of relationship it is and bind value
            else if (currentGrailsProp.isManyToOne()) {
                if (LOG.isDebugEnabled())
                    LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as ManyToOne");

                value = new ManyToOne(mappings, table);
                bindManyToOne(currentGrailsProp, (ManyToOne) value, EMPTY_PATH, mappings, sessionFactoryBeanName);
            }
            else if (currentGrailsProp.isOneToOne() && userType == null) {
                if (LOG.isDebugEnabled())
                    LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as OneToOne");

                if (currentGrailsProp.isHasOne()&&!currentGrailsProp.isBidirectional()) {
                   throw new MappingException("hasOne property [" + currentGrailsProp.getDomainClass().getName() +
                               "." + currentGrailsProp.getName() + "] is not bidirectional. Specify the other side of the relationship!");
                }
                else if (canBindOneToOneWithSingleColumnAndForeignKey(currentGrailsProp)) {
                    value = new OneToOne(mappings, table, persistentClass);
                    bindOneToOne(currentGrailsProp, (OneToOne) value, EMPTY_PATH, sessionFactoryBeanName);
                }
                else {
                    if (currentGrailsProp.isHasOne() && currentGrailsProp.isBidirectional()) {
                        value = new OneToOne(mappings, table, persistentClass);
                        bindOneToOne(currentGrailsProp, (OneToOne) value, EMPTY_PATH, sessionFactoryBeanName);
                    }
                    else {
                        value = new ManyToOne(mappings, table);
                        bindManyToOne(currentGrailsProp, (ManyToOne) value, EMPTY_PATH, mappings, sessionFactoryBeanName);
                    }
                }
            }
            else if (currentGrailsProp.isEmbedded()) {
                value = new Component(mappings, persistentClass);

                bindComponent((Component) value, currentGrailsProp, true, mappings, sessionFactoryBeanName);
            }
            else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as SimpleValue");
                }
                value = new SimpleValue(mappings, table);
                bindSimpleValue(currentGrailsProp, null, (SimpleValue) value, EMPTY_PATH, mappings, sessionFactoryBeanName);
            }

            if (value != null) {
                Property property = createProperty(value, persistentClass, currentGrailsProp, mappings);
                persistentClass.addProperty(property);
            }
        }

        bindNaturalIdentifier(table, gormMapping, persistentClass);
    }

    protected void bindNaturalIdentifier(Table table, Mapping mapping, PersistentClass persistentClass) {
        Object o = mapping != null ? mapping.getIdentity() : null;
        if (!(o instanceof Identity)) {
            return;
        }

        Identity identity = (Identity) o;
        final NaturalId naturalId = identity.getNatural();
        if (naturalId == null || naturalId.getPropertyNames().isEmpty()) {
            return;
        }

        UniqueKey uk = new UniqueKey();
        uk.setTable(table);

        boolean mutable = naturalId.isMutable();

        for (String propertyName : naturalId.getPropertyNames()) {
            Property property = persistentClass.getProperty(propertyName);

            property.setNaturalIdentifier(true);
            if (!mutable) property.setUpdateable(false);

            uk.addColumns(property.getColumnIterator());
        }

        setUniqueName(uk);

        table.addUniqueKey(uk);
    }

    protected void setUniqueName(UniqueKey uk) {
        StringBuilder sb = new StringBuilder(uk.getTable().getName()).append('_');
        for (Object col : uk.getColumns()) {
            sb.append(((Column) col).getName()).append('_');
        }

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try {
            md.update(sb.toString().getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        String name = "UK" + new BigInteger(1, md.digest()).toString(16);
        if (name.length() > 30) {
            // Oracle has a 30-char limit
            name = name.substring(0, 30);
        }

        uk.setName(name);
    }

    protected boolean canBindOneToOneWithSingleColumnAndForeignKey(GrailsDomainClassProperty currentGrailsProp) {
        if (currentGrailsProp.isBidirectional()) {
            final GrailsDomainClassProperty otherSide = currentGrailsProp.getOtherSide();
            if (otherSide.isHasOne()) return false;
            if (!currentGrailsProp.isOwningSide() && (otherSide.isOwningSide())) {
                return true;
            }
        }
        return false;
    }

    protected boolean isIdentityProperty(Mapping gormMapping, GrailsDomainClassProperty currentGrailsProp) {
        if (gormMapping == null) {
            return false;
        }

        Object identityMapping = gormMapping.getIdentity();
        if (!(identityMapping instanceof Identity)) {
            return false;
        }

        String identityName = ((Identity)identityMapping).getName();
        return identityName != null && identityName.equals(currentGrailsProp.getName());
    }

    protected void bindEnumType(GrailsDomainClassProperty property, SimpleValue simpleValue,
            String path, String sessionFactoryBeanName) {
        bindEnumType(property, property.getType(), simpleValue,
                getColumnNameForPropertyAndPath(property, path, null, sessionFactoryBeanName));
    }

    protected void bindEnumType(GrailsDomainClassProperty property, Class<?> propertyType, SimpleValue simpleValue, String columnName) {

        PropertyConfig pc = getPropertyConfig(property);
        String typeName = getTypeName(property, getPropertyConfig(property), getMapping(property.getDomainClass()));
        if (typeName == null) {
            Properties enumProperties = new Properties();
            enumProperties.put(ENUM_CLASS_PROP, propertyType.getName());

            String enumType = pc == null ? DEFAULT_ENUM_TYPE : pc.getEnumType();
            if (enumType.equals(DEFAULT_ENUM_TYPE) && identityEnumTypeSupports(propertyType)) {
                simpleValue.setTypeName("org.codehaus.groovy.grails.orm.hibernate.cfg.IdentityEnumType");
            } else {
                simpleValue.setTypeName(ENUM_TYPE_CLASS);
                if (enumType.equals(DEFAULT_ENUM_TYPE) || "string".equalsIgnoreCase(enumType)) {
                    enumProperties.put(ENUM_TYPE_PROP, String.valueOf(Types.VARCHAR));
                }
                else if (!"ordinal".equalsIgnoreCase(enumType)) {
                    LOG.warn("Invalid enumType specified when mapping property [" + property.getName() +
                            "] of class [" + property.getDomainClass().getClazz().getName() +
                            "]. Using defaults instead.");
                }
            }
            simpleValue.setTypeParameters(enumProperties);
        }
        else {
            simpleValue.setTypeName(typeName);
        }

        Table t = simpleValue.getTable();
        Column column = new Column();

        if (property.getDomainClass().isRoot()) {
            column.setNullable(property.isOptional());
        } else {
            Mapping mapping = getMapping(property.getDomainClass());
            if (mapping == null || mapping.getTablePerHierarchy()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[GrailsDomainBinder] Sub class property [" + property.getName() +
                            "] for column name [" + column.getName() + "] set to nullable");
                }
                column.setNullable(true);
            } else {
                column.setNullable(property.isOptional());
            }
        }
        column.setValue(simpleValue);
        column.setName(columnName);
        if (t != null) t.addColumn(column);

        simpleValue.addColumn(column);

        PropertyConfig propertyConfig = getPropertyConfig(property);
        if (propertyConfig != null && !propertyConfig.getColumns().isEmpty()) {
            bindIndex(columnName, column, propertyConfig.getColumns().get(0), t);
            bindColumnConfigToColumn(column, propertyConfig.getColumns().get(0));
        }
    }

    protected Class<?> getUserType(GrailsDomainClassProperty currentGrailsProp) {
        Class<?> userType = null;
        PropertyConfig config = getPropertyConfig(currentGrailsProp);
        Object typeObj = config == null ? null : config.getType();
        if (typeObj instanceof Class<?>) {
            userType = (Class<?>)typeObj;
        } else if (typeObj != null) {
            String typeName = typeObj.toString();
            try {
                userType = Class.forName(typeName, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                // only print a warning if the user type is in a package this excludes basic
                // types like string, int etc.
                if (typeName.indexOf(".")>-1) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("UserType not found ", e);
                    }
                }
            }
        }
        return userType;
    }

    protected boolean isCompositeIdProperty(Mapping gormMapping, GrailsDomainClassProperty currentGrailsProp) {
        if (gormMapping != null && gormMapping.getIdentity() != null) {
            Object id = gormMapping.getIdentity();
            if (id instanceof CompositeIdentity) {
                if (ArrayUtils.contains(((CompositeIdentity)id).getPropertyNames(), currentGrailsProp.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isBidirectionalManyToOne(GrailsDomainClassProperty currentGrailsProp) {
        return (currentGrailsProp.isBidirectional() && currentGrailsProp.isManyToOne());
    }

    /**
     * Binds a Hibernate component type using the given GrailsDomainClassProperty instance
     *
     * @param component  The component to bind
     * @param property   The property
     * @param isNullable Whether it is nullable or not
     * @param mappings   The Hibernate Mappings object
     * @param sessionFactoryBeanName  the session factory bean name
     */
    protected void bindComponent(Component component, GrailsDomainClassProperty property,
            boolean isNullable, Mappings mappings, String sessionFactoryBeanName) {
        component.setEmbedded(true);
        Class<?> type = property.getType();
        String role = qualify(type.getName(), property.getName());
        component.setRoleName(role);
        component.setComponentClassName(type.getName());

        GrailsDomainClass domainClass = property.getReferencedDomainClass() != null ? property.getReferencedDomainClass() : property.getComponent();

        evaluateMapping(domainClass);
        GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties();
        Table table = component.getOwner().getTable();
        PersistentClass persistentClass = component.getOwner();
        String path = property.getName();
        Class<?> propertyType = property.getDomainClass().getClazz();

        for (GrailsDomainClassProperty currentGrailsProp : properties) {
            if (currentGrailsProp.isIdentity()) continue;
            if (currentGrailsProp.getName().equals(GrailsDomainClassProperty.VERSION)) continue;

            if (currentGrailsProp.getType().equals(propertyType)) {
                component.setParentProperty(currentGrailsProp.getName());
                continue;
            }

            bindComponentProperty(component, property, currentGrailsProp, persistentClass, path,
                    table, mappings, sessionFactoryBeanName);
        }
    }

    protected void bindComponentProperty(Component component, GrailsDomainClassProperty componentProperty,
             GrailsDomainClassProperty currentGrailsProp, PersistentClass persistentClass,
             String path, Table table, Mappings mappings, String sessionFactoryBeanName) {
        Value value;
        // see if it's a collection type
        CollectionType collectionType = CT.collectionTypeForClass(currentGrailsProp.getType());
        if (collectionType != null) {
            // create collection
            Collection collection = collectionType.create(currentGrailsProp, persistentClass,
                    path, mappings, sessionFactoryBeanName);
            mappings.addCollection(collection);
            value = collection;
        }
        // work out what type of relationship it is and bind value
        else if (currentGrailsProp.isManyToOne()) {
            if (LOG.isDebugEnabled())
                LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as ManyToOne");

            value = new ManyToOne(mappings, table);
            bindManyToOne(currentGrailsProp, (ManyToOne) value, path, mappings, sessionFactoryBeanName);
        } else if (currentGrailsProp.isOneToOne()) {
            if (LOG.isDebugEnabled())
                LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as OneToOne");

           if (canBindOneToOneWithSingleColumnAndForeignKey(currentGrailsProp)) {
                value = new OneToOne(mappings, table, persistentClass);
                bindOneToOne(currentGrailsProp, (OneToOne) value, path, sessionFactoryBeanName);
            }
            else {
                value = new ManyToOne(mappings, table);
                bindManyToOne(currentGrailsProp, (ManyToOne) value, path, mappings, sessionFactoryBeanName);
            }
        }
        else if (currentGrailsProp.isEmbedded()) {
            value = new Component(mappings, persistentClass);
            bindComponent((Component) value, currentGrailsProp, true, mappings, sessionFactoryBeanName);
        }
        else {
            if (LOG.isDebugEnabled())
                LOG.debug("[GrailsDomainBinder] Binding property [" + currentGrailsProp.getName() + "] as SimpleValue");

            value = new SimpleValue(mappings, table);
            if (currentGrailsProp.isEnum()) {
                bindEnumType(currentGrailsProp, (SimpleValue) value, path, sessionFactoryBeanName);
            }
            else {
                bindSimpleValue(currentGrailsProp, componentProperty, (SimpleValue) value, path,
                        mappings, sessionFactoryBeanName);
            }
        }

        if (value != null) {
            Property persistentProperty = createProperty(value, persistentClass, currentGrailsProp, mappings);
            component.addProperty(persistentProperty);
            if (isComponentPropertyNullable(componentProperty)) {
                final Iterator<?> columnIterator = value.getColumnIterator();
                while (columnIterator.hasNext()) {
                    Column c = (Column) columnIterator.next();
                    c.setNullable(true);
                }
            }
        }
    }

    protected boolean isComponentPropertyNullable(GrailsDomainClassProperty componentProperty) {
        if (componentProperty == null) return false;
        final GrailsDomainClass domainClass = componentProperty.getDomainClass();
        final Mapping mapping = getMapping(domainClass.getClazz());
        return !domainClass.isRoot() && (mapping == null || mapping.isTablePerHierarchy()) || componentProperty.isOptional();
    }

    /*
     * Creates a persistent class property based on the GrailDomainClassProperty instance.
     */
    protected Property createProperty(Value value, PersistentClass persistentClass, GrailsDomainClassProperty grailsProperty, Mappings mappings) {
        // set type
        value.setTypeUsingReflection(persistentClass.getClassName(), grailsProperty.getName());

        if (value.getTable() != null) {
            value.createForeignKey();
        }

        Property prop = new Property();
        prop.setValue(value);
        bindProperty(grailsProperty, prop, mappings);
        return prop;
    }

    protected void bindOneToMany(GrailsDomainClassProperty currentGrailsProp, OneToMany one, Mappings mappings) {
        one.setReferencedEntityName(currentGrailsProp.getReferencedPropertyType().getName());
        one.setIgnoreNotFound(true);
    }

    /**
     * Binds a many-to-one relationship to the
     *
     */
    @SuppressWarnings("unchecked")
    protected void bindManyToOne(GrailsDomainClassProperty property, ManyToOne manyToOne,
             String path, Mappings mappings, String sessionFactoryBeanName) {

        NamingStrategy namingStrategy = getNamingStrategy(sessionFactoryBeanName);

        bindManyToOneValues(property, manyToOne);
        GrailsDomainClass refDomainClass = property.isManyToMany() ? property.getDomainClass() : property.getReferencedDomainClass();
        Mapping mapping = getMapping(refDomainClass);
        boolean isComposite = hasCompositeIdentifier(mapping);
        if (isComposite) {
            CompositeIdentity ci = (CompositeIdentity) mapping.getIdentity();
            bindCompositeIdentifierToManyToOne(property, manyToOne, ci, refDomainClass, path, sessionFactoryBeanName);
        }
        else {
            if (property.isCircular() && property.isManyToMany()) {
                PropertyConfig pc = getPropertyConfig(property);

                if (pc == null) {
                    if (mapping == null) {
                        mapping = new Mapping();
                        MAPPING_CACHE.put(refDomainClass.getClazz(), mapping);
                    }
                    pc = new PropertyConfig();
                    mapping.getColumns().put(property.getName(), pc);
                }
                if (!hasJoinKeyMapping(pc)) {
                    JoinTable jt = new JoinTable();
                    final ColumnConfig columnConfig = new ColumnConfig();
                    columnConfig.setName(namingStrategy.propertyToColumnName(property.getName()) +
                            UNDERSCORE + FOREIGN_KEY_SUFFIX);
                    jt.setKey(columnConfig);
                    pc.setJoinTable(jt);
                }
                bindSimpleValue(property, manyToOne, path, pc, sessionFactoryBeanName);
            }
            else {
                // bind column
                bindSimpleValue(property, null, manyToOne, path, mappings, sessionFactoryBeanName);
            }
        }

        PropertyConfig config = getPropertyConfig(property);
        if (property.isOneToOne() && !isComposite) {
            manyToOne.setAlternateUniqueKey(true);
            Column c = getColumnForSimpleValue(manyToOne);
            if (config != null) {
                c.setUnique(config.isUnique());
            }
            else if (property.isBidirectional() && property.getOtherSide().isHasOne()) {
                c.setUnique(true);
            }
        }
    }

    protected void bindCompositeIdentifierToManyToOne(GrailsDomainClassProperty property,
            SimpleValue value, CompositeIdentity compositeId, GrailsDomainClass refDomainClass,
            String path, String sessionFactoryBeanName) {

        NamingStrategy namingStrategy = getNamingStrategy(sessionFactoryBeanName);

        String[] propertyNames = compositeId.getPropertyNames();
        PropertyConfig config = getPropertyConfig(property);
        if (config == null) config = new PropertyConfig();

        if (config.getColumns().size() != propertyNames.length) {
            for (String propertyName : propertyNames) {
                final ColumnConfig cc = new ColumnConfig();
                cc.setName(addUnderscore(namingStrategy.classToTableName(refDomainClass.getShortName()),
                        getDefaultColumnName(refDomainClass.getPropertyByName(propertyName), sessionFactoryBeanName)));
                config.getColumns().add(cc);
            }
        }
        bindSimpleValue(property, value, path, config, sessionFactoryBeanName);
    }

    protected boolean hasCompositeIdentifier(Mapping mapping) {
        return mapping != null && (mapping.getIdentity() instanceof CompositeIdentity);
    }

    protected void bindOneToOne(final GrailsDomainClassProperty property, OneToOne oneToOne,
            String path, String sessionFactoryBeanName) {
        PropertyConfig config = getPropertyConfig(property);
        final GrailsDomainClassProperty otherSide = property.getOtherSide();

        oneToOne.setConstrained(otherSide.isHasOne());
        oneToOne.setForeignKeyType(oneToOne.isConstrained() ?
                                   ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT :
                                   ForeignKeyDirection.FOREIGN_KEY_TO_PARENT);
        oneToOne.setAlternateUniqueKey(true);

        if (config != null && config.getFetch() != null) {
            oneToOne.setFetchMode(config.getFetch());
        }
        else {
            oneToOne.setFetchMode(FetchMode.DEFAULT);
        }

        oneToOne.setReferencedEntityName(otherSide.getDomainClass().getFullName());
        oneToOne.setPropertyName(property.getName());

        if (otherSide.isHasOne()) {
            PropertyConfig pc = getPropertyConfig(property);
            bindSimpleValue(property, oneToOne, path, pc, sessionFactoryBeanName);
        }
        else {
            oneToOne.setReferencedPropertyName(otherSide.getName());
        }
    }

    /**
     */
    protected void bindManyToOneValues(GrailsDomainClassProperty property, ManyToOne manyToOne) {
        PropertyConfig config = getPropertyConfig(property);

        if (config != null && config.getFetch() != null) {
            manyToOne.setFetchMode(config.getFetch());
        }
        else {
            manyToOne.setFetchMode(FetchMode.DEFAULT);
        }

        manyToOne.setLazy(getLaziness(property));

        if (config != null) {
           manyToOne.setIgnoreNotFound(config.getIgnoreNotFound());
        }

        // set referenced entity
        manyToOne.setReferencedEntityName(property.getReferencedPropertyType().getName());
    }

    protected void bindVersion(GrailsDomainClassProperty version, RootClass entity,
            Mappings mappings, String sessionFactoryBeanName) {

        SimpleValue val = new SimpleValue(mappings, entity.getTable());

        bindSimpleValue(version, null, val, EMPTY_PATH, mappings, sessionFactoryBeanName);

        if (val.isTypeSpecified()) {
            if (!(val.getType() instanceof IntegerType ||
                  val.getType() instanceof LongType ||
                  val.getType() instanceof TimestampType)) {
                LOG.warn("Invalid version class specified in " + version.getDomainClass().getClazz().getName() +
                         "; must be one of [int, Integer, long, Long, Timestamp, Date]. Not mapping the version.");
                return;
            }
        }
        else {
            val.setTypeName("version".equals(version.getName()) ? "integer" : "timestamp");
        }
        Property prop = new Property();
        prop.setValue(val);

        bindProperty(version, prop, mappings);
        val.setNullValue("undefined");
        entity.setVersion(prop);
        entity.addProperty(prop);
    }

    @SuppressWarnings("unchecked")
    protected void bindSimpleId(GrailsDomainClassProperty identifier, RootClass entity,
            Mappings mappings, Identity mappedId, String sessionFactoryBeanName) {

        Mapping mapping = getMapping(identifier.getDomainClass());
        boolean useSequence = mapping != null && mapping.isTablePerConcreteClass();

        // create the id value
        SimpleValue id = new SimpleValue(mappings, entity.getTable());
        // set identifier on entity

        Properties params = new Properties();
        entity.setIdentifier(id);

        if (mappedId == null) {
            // configure generator strategy
            id.setIdentifierGeneratorStrategy(useSequence ? "sequence-identity" : "native");
        } else {
            String generator = mappedId.getGenerator();
            if("native".equals(generator) && useSequence) {
                generator = "sequence-identity";
            }
            id.setIdentifierGeneratorStrategy(generator);
            params.putAll(mappedId.getParams());
            if ("assigned".equals(generator)) {
                id.setNullValue("undefined");
            }
        }

        params.put(PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER, mappings.getObjectNameNormalizer());

        if (mappings.getSchemaName() != null) {
            params.setProperty(PersistentIdentifierGenerator.SCHEMA, mappings.getSchemaName());
        }
        if (mappings.getCatalogName() != null) {
            params.setProperty(PersistentIdentifierGenerator.CATALOG, mappings.getCatalogName());
        }
        id.setIdentifierGeneratorProperties(params);

        // bind value
        bindSimpleValue(identifier, null, id, EMPTY_PATH, mappings, sessionFactoryBeanName);

        // create property
        Property prop = new Property();
        prop.setValue(id);

        // bind property
        bindProperty(identifier, prop, mappings);
        // set identifier property
        entity.setIdentifierProperty(prop);

        id.getTable().setIdentifierValue(id);
    }

    /**
     * Binds a property to Hibernate runtime meta model. Deals with cascade strategy based on the Grails domain model
     *
     * @param grailsProperty The grails property instance
     * @param prop           The Hibernate property
     * @param mappings       The Hibernate mappings
     */
    protected void bindProperty(GrailsDomainClassProperty grailsProperty, Property prop, Mappings mappings) {
        // set the property name
        prop.setName(grailsProperty.getName());
        if (isBidirectionalManyToOneWithListMapping(grailsProperty, prop)) {
            prop.setInsertable(false);
            prop.setUpdateable(false);
        } else {
            prop.setInsertable(getInsertableness(grailsProperty));
            prop.setUpdateable(getUpdateableness(grailsProperty));
        }

        prop.setPropertyAccessorName(mappings.getDefaultAccess());
        prop.setOptional(grailsProperty.isOptional());

        setCascadeBehaviour(grailsProperty, prop);

        // lazy to true
        boolean isLazyable = grailsProperty.isOneToOne() ||
                             grailsProperty.isManyToOne() ||
                             grailsProperty.isEmbedded() ||
                             grailsProperty.isPersistent() && !grailsProperty.isAssociation() && !grailsProperty.isIdentity();

        if (isLazyable) {
            final boolean isLazy = getLaziness(grailsProperty);
            prop.setLazy(isLazy);

            if (isLazy && (grailsProperty.isManyToOne() || grailsProperty.isOneToOne())) {
                handleLazyProxy(grailsProperty.getDomainClass(), grailsProperty);
            }
        }
    }

    protected boolean getLaziness(GrailsDomainClassProperty grailsProperty) {
        PropertyConfig config = getPropertyConfig(grailsProperty);
        return config == null ? true : config.getLazy();
    }

    protected boolean getInsertableness(GrailsDomainClassProperty grailsProperty) {
        PropertyConfig config = getPropertyConfig(grailsProperty);
        return config == null ? true : config.getInsertable();
    }

    protected boolean getUpdateableness(GrailsDomainClassProperty grailsProperty) {
        PropertyConfig config = getPropertyConfig(grailsProperty);
        return config == null ? true : config.getUpdateable();
    }

    protected boolean isBidirectionalManyToOneWithListMapping(GrailsDomainClassProperty grailsProperty, Property prop) {
        GrailsDomainClassProperty otherSide = grailsProperty.getOtherSide();
        return grailsProperty.isBidirectional() && otherSide != null &&
                 prop.getValue() instanceof ManyToOne &&
                 List.class.isAssignableFrom(otherSide.getType());
    }

    protected void setCascadeBehaviour(GrailsDomainClassProperty grailsProperty, Property prop) {
        String cascadeStrategy = "none";
        // set to cascade all for the moment
        GrailsDomainClass domainClass = grailsProperty.getDomainClass();
        PropertyConfig config = getPropertyConfig(grailsProperty);
        GrailsDomainClass referenced = grailsProperty.getReferencedDomainClass();
        if (config != null && config.getCascade() != null) {
            cascadeStrategy = config.getCascade();
        } else if (grailsProperty.isAssociation()) {
            if (grailsProperty.isHasOne()) {
               cascadeStrategy = CASCADE_ALL;
            }
            else if (grailsProperty.isOneToOne()) {
                if (referenced != null && referenced.isOwningClass(domainClass.getClazz())) {
                    cascadeStrategy = CASCADE_ALL;
                }
            } else if (grailsProperty.isOneToMany()) {
                if (referenced != null && referenced.isOwningClass(domainClass.getClazz())) {
                    cascadeStrategy = CASCADE_ALL;
                }
                else {
                    cascadeStrategy = CASCADE_SAVE_UPDATE;
                }
            } else if (grailsProperty.isManyToMany()) {
                if ((referenced != null && referenced.isOwningClass(domainClass.getClazz())) || grailsProperty.isCircular()) {
                    cascadeStrategy = CASCADE_SAVE_UPDATE;
                }
            } else if (grailsProperty.isManyToOne()) {
                if (referenced != null && referenced.isOwningClass(domainClass.getClazz()) && !isCircularAssociation(grailsProperty)) {
                    cascadeStrategy = CASCADE_ALL;
                }
                else {
                    cascadeStrategy = CASCADE_NONE;
                }
            }
        }
        else if (grailsProperty.isBasicCollectionType()) {
            cascadeStrategy = CASCADE_ALL;
        }
        else if (Map.class.isAssignableFrom(grailsProperty.getType())) {
            referenced = grailsProperty.getReferencedDomainClass();
            if (referenced != null && referenced.isOwningClass(grailsProperty.getDomainClass().getClazz())) {
                cascadeStrategy = CASCADE_ALL;
            } else {
                cascadeStrategy = CASCADE_SAVE_UPDATE;
            }
        }
        logCascadeMapping(grailsProperty, cascadeStrategy, referenced);
        prop.setCascade(cascadeStrategy);
    }

    protected boolean isCircularAssociation(GrailsDomainClassProperty grailsProperty) {
        return grailsProperty.getType().equals(grailsProperty.getDomainClass().getClazz());
    }

    protected void logCascadeMapping(GrailsDomainClassProperty grailsProperty, String cascadeStrategy, GrailsDomainClass referenced) {
        if (LOG.isDebugEnabled() && grailsProperty.isAssociation() && referenced != null) {
            String assType = getAssociationDescription(grailsProperty);
            LOG.debug("Mapping cascade strategy for " + assType + " property " + grailsProperty.getDomainClass().getFullName() + "." + grailsProperty.getName() + " referencing type [" + referenced.getClazz() + "] -> [CASCADE: " + cascadeStrategy + "]");
        }
    }

    protected String getAssociationDescription(GrailsDomainClassProperty grailsProperty) {
        String assType = "unknown";
        if (grailsProperty.isManyToMany()) {
            assType = "many-to-many";
        } else if (grailsProperty.isOneToMany()) {
            assType = "one-to-many";
        } else if (grailsProperty.isOneToOne()) {
            assType = "one-to-one";
        } else if (grailsProperty.isManyToOne()) {
            assType = "many-to-one";
        } else if (grailsProperty.isEmbedded()) {
            assType = "embedded";
        }
        return assType;
    }

    /**
     * Binds a simple value to the Hibernate metamodel. A simple value is
     * any type within the Hibernate type system
     *
     * @param property
     * @param parentProperty
     * @param simpleValue The simple value to bind
     * @param path
     * @param mappings    The Hibernate mappings instance
     * @param sessionFactoryBeanName  the session factory bean name
     */
    protected void bindSimpleValue(GrailsDomainClassProperty property, GrailsDomainClassProperty parentProperty,
            SimpleValue simpleValue, String path, Mappings mappings, String sessionFactoryBeanName) {
        // set type
        bindSimpleValue(property,parentProperty, simpleValue, path, getPropertyConfig(property), sessionFactoryBeanName);
    }

    protected void bindSimpleValue(GrailsDomainClassProperty grailsProp, SimpleValue simpleValue,
            String path, PropertyConfig propertyConfig, String sessionFactoryBeanName) {
        bindSimpleValue(grailsProp, null, simpleValue, path, propertyConfig, sessionFactoryBeanName);
    }

    protected void bindSimpleValue(GrailsDomainClassProperty grailsProp,
            GrailsDomainClassProperty parentProperty, SimpleValue simpleValue,
            String path, PropertyConfig propertyConfig, String sessionFactoryBeanName) {
        setTypeForPropertyConfig(grailsProp, simpleValue, propertyConfig);
        if (grailsProp.isDerived()) {
            Formula formula = new Formula();
            formula.setFormula(propertyConfig.getFormula());
            simpleValue.addFormula(formula);
        } else {
            Table table = simpleValue.getTable();

            // Add the column definitions for this value/property. Note that
            // not all custom mapped properties will have column definitions,
            // in which case we still need to create a Hibernate column for
            // this value.
            List<?> columnDefinitions = propertyConfig != null ? propertyConfig.getColumns()
                    : Arrays.asList(new Object[] { null });
            if (columnDefinitions.isEmpty()) {
                columnDefinitions = Arrays.asList(new Object[] { null });
            }

            for (int i = 0, n = columnDefinitions.size(); i < n; i++) {
                ColumnConfig cc = (ColumnConfig) columnDefinitions.get(i);
                Column column = new Column();

                // Check for explicitly mapped column name and SQL type.
                if (cc != null) {
                    if (cc.getName() != null) {
                        column.setName(cc.getName());
                    }
                    if (cc.getSqlType() != null) {
                        column.setSqlType(cc.getSqlType());
                    }
                }

                column.setValue(simpleValue);


                if (cc != null) {
                    if (cc.getLength() != -1) {
                        column.setLength(cc.getLength());
                    }
                    if (cc.getPrecision() != -1) {
                        column.setPrecision(cc.getPrecision());
                    }
                    if (cc.getScale() != -1) {
                        column.setScale(cc.getScale());
                    }
                    column.setUnique(cc.isUnique());
                }

                bindColumn(grailsProp, parentProperty, column, cc, path, table, sessionFactoryBeanName);

                if (table != null) {
                    table.addColumn(column);
                }

                simpleValue.addColumn(column);
            }
        }
    }

    protected void setTypeForPropertyConfig(GrailsDomainClassProperty grailsProp, SimpleValue simpleValue, PropertyConfig config) {
        final String typeName = getTypeName(grailsProp, getPropertyConfig(grailsProp), getMapping(grailsProp.getDomainClass()));
        if (typeName == null) {
            simpleValue.setTypeName(grailsProp.getType().getName());
        }
        else {
            simpleValue.setTypeName(typeName);
            if (config != null) {
                simpleValue.setTypeParameters(config.getTypeParams());
            }
        }
    }

    /**
     * Binds a value for the specified parameters to the meta model.
     *
     * @param type        The type of the property
     * @param simpleValue The simple value instance
     * @param nullable    Whether it is nullable
     * @param columnName  The property name
     * @param mappings    The mappings
     */
    protected void bindSimpleValue(String type, SimpleValue simpleValue, boolean nullable,
            String columnName, Mappings mappings) {

        simpleValue.setTypeName(type);
        Table t = simpleValue.getTable();
        Column column = new Column();
        column.setNullable(nullable);
        column.setValue(simpleValue);
        column.setName(columnName);
        if (t != null) t.addColumn(column);

        simpleValue.addColumn(column);
    }

    /**
     * Binds a Column instance to the Hibernate meta model
     *
     * @param property The Grails domain class property
     * @param parentProperty
     * @param column     The column to bind
     * @param path
     * @param table      The table name
     * @param sessionFactoryBeanName  the session factory bean name
     */
    protected void bindColumn(GrailsDomainClassProperty property, GrailsDomainClassProperty parentProperty,
            Column column, ColumnConfig cc, String path, Table table, String sessionFactoryBeanName) {

        if (cc != null) {
            column.setComment(cc.getComment());
            column.setDefaultValue(cc.getDefaultValue());
        }

        Class<?> userType = getUserType(property);
        String columnName = getColumnNameForPropertyAndPath(property, path, cc, sessionFactoryBeanName);
        if ((property.isAssociation() || property.isBasicCollectionType()) && userType == null) {
            // Only use conventional naming when the column has not been explicitly mapped.
            if (column.getName() == null) {
                column.setName(columnName);
            }
            if (property.isManyToMany()) {
                column.setNullable(false);
            }
            else if (property.isOneToOne() && property.isBidirectional() && !property.isOwningSide()) {
                if (property.getOtherSide().isHasOne()) {
                    column.setNullable(false);
                }
                else {
                    column.setNullable(true);
                }
            }
            else if ((property.isManyToOne() || property.isOneToOne()) && property.isCircular()) {
                column.setNullable(true);
            }
            else {
                column.setNullable(property.isOptional());
            }
        }
        else {
            column.setName(columnName);
            column.setNullable(property.isOptional() || (parentProperty != null && parentProperty.isOptional()));

            // Use the constraints for this property to more accurately define
            // the column's length, precision, and scale
            ConstrainedProperty constrainedProperty = getConstrainedProperty(property);
            if (constrainedProperty != null) {
                if (String.class.isAssignableFrom(property.getType()) || byte[].class.isAssignableFrom(property.getType())) {
                    bindStringColumnConstraints(column, constrainedProperty);
                }

                if (Number.class.isAssignableFrom(property.getType())) {
                    bindNumericColumnConstraints(column, constrainedProperty, cc);
                }
            }
        }

        handleUniqueConstraint(property, column, path, table, columnName, sessionFactoryBeanName);

        bindIndex(columnName, column, cc, table);

        if (!property.getDomainClass().isRoot()) {
            Mapping mapping = getMapping(property.getDomainClass());
            if (mapping == null || mapping.getTablePerHierarchy()) {
                if (LOG.isDebugEnabled())
                    LOG.debug("[GrailsDomainBinder] Sub class property [" + property.getName() + "] for column name ["+column.getName()+"] set to nullable");
                column.setNullable(true);
            } else {
                column.setNullable(property.isOptional());
            }
        }

        if (LOG.isDebugEnabled())
            LOG.debug("[GrailsDomainBinder] bound property [" + property.getName() + "] to column name ["+column.getName()+"] in table ["+table.getName()+"]");
    }

    protected abstract void handleUniqueConstraint(GrailsDomainClassProperty property, Column column, String path, Table table, String columnName, String sessionFactoryBeanName);

    protected void createKeyForProps(GrailsDomainClassProperty grailsProp, String path, Table table,
            String columnName, List<?> propertyNames, String sessionFactoryBeanName) {
        List<Column> keyList = new ArrayList<Column>();
        keyList.add(new Column(columnName));
        for (Iterator<?> i = propertyNames.iterator(); i.hasNext();) {
            String propertyName = (String) i.next();
            GrailsDomainClassProperty otherProp = grailsProp.getDomainClass().getPropertyByName(propertyName);
            String otherColumnName = getColumnNameForPropertyAndPath(otherProp, path, null, sessionFactoryBeanName);
            keyList.add(new Column(otherColumnName));
        }
        createUniqueKeyForColumns(table, columnName, keyList);
    }

    protected void createUniqueKeyForColumns(Table table, String columnName, List<Column> keyList) {
        Collections.reverse(keyList);
        UniqueKey key = table.getOrCreateUniqueKey("unique-" + columnName);
        List<?> columns = key.getColumns();
        if (columns.isEmpty()) {
            LOG.debug("create unique key for " + table.getName() + " columns = " + keyList);
            key.addColumns(keyList.iterator());
        }
    }

    protected void bindIndex(String columnName, Column column, ColumnConfig cc, Table table) {
        if (cc == null) {
            return;
        }

        Object indexObj = cc.getIndex();
        String indexDefinition = null;
        if (indexObj instanceof Boolean) {
            Boolean b = (Boolean) indexObj;
            if (b) {
               indexDefinition = columnName + "_idx";
            }
        }
        else if (indexObj != null) {
            indexDefinition = indexObj.toString();
        }
        if (indexDefinition == null) {
            return;
        }

        String[] tokens = indexDefinition.split(",");
        for (int i = 0; i < tokens.length; i++) {
            String index = tokens[i];
            table.getOrCreateIndex(index).addColumn(column);
        }
    }

    protected String getColumnNameForPropertyAndPath(GrailsDomainClassProperty grailsProp,
            String path, ColumnConfig cc, String sessionFactoryBeanName) {

        NamingStrategy namingStrategy = getNamingStrategy(sessionFactoryBeanName);

        // First try the column config.
        String columnName = null;
        if (cc == null) {
            // No column config given, so try to fetch it from the mapping
            GrailsDomainClass domainClass = grailsProp.getDomainClass();
            Mapping m = getMapping(domainClass.getClazz());
            if (m != null) {
                PropertyConfig c = m.getPropertyConfig(grailsProp.getName());

                if (supportsJoinColumnMapping(grailsProp) && hasJoinKeyMapping(c)) {
                   columnName = c.getJoinTable().getKey().getName();
                }
                else if (c != null && c.getColumn() != null) {
                    columnName = c.getColumn();
                }
            }
        }
        else {
            if (supportsJoinColumnMapping(grailsProp)) {
                PropertyConfig pc = getPropertyConfig(grailsProp);
                if (hasJoinKeyMapping(pc)) {
                    columnName = pc.getJoinTable().getKey().getName();
                }
                else {
                    columnName = cc.getName();
                }
            }
            else {
                columnName = cc.getName();
            }
        }

        if (columnName == null) {
            if (isNotEmpty(path)) {
                columnName = addUnderscore(namingStrategy.propertyToColumnName(path),
                        getDefaultColumnName(grailsProp, sessionFactoryBeanName));
            } else {
                columnName = getDefaultColumnName(grailsProp, sessionFactoryBeanName);
            }
        }
        return columnName;
    }

    protected boolean hasJoinKeyMapping(PropertyConfig c) {
        return c != null && c.getJoinTable() != null && c.getJoinTable().getKey() != null;
    }

    protected boolean supportsJoinColumnMapping(GrailsDomainClassProperty grailsProp) {
        return grailsProp.isManyToMany() || isUnidirectionalOneToMany(grailsProp) || grailsProp.isBasicCollectionType();
    }

    protected String getDefaultColumnName(GrailsDomainClassProperty property, String sessionFactoryBeanName) {

        NamingStrategy namingStrategy = getNamingStrategy(sessionFactoryBeanName);

        String columnName = namingStrategy.propertyToColumnName(property.getName());
        if (property.isAssociation() && property.getReferencedDomainClass() != null) {

            if (property.isManyToMany()) {
                return getForeignKeyForPropertyDomainClass(property, sessionFactoryBeanName);
            }

            if (!property.isBidirectional() && property.isOneToMany()) {
                String prefix = namingStrategy.classToTableName(property.getDomainClass().getName());
                return addUnderscore(prefix, columnName) + FOREIGN_KEY_SUFFIX;
            }

            if (property.isInherited() && isBidirectionalManyToOne(property)) {
                return namingStrategy.propertyToColumnName(property.getDomainClass().getName()) + '_'+ columnName + FOREIGN_KEY_SUFFIX;
            }

            return columnName + FOREIGN_KEY_SUFFIX;
        }

        if (property.isBasicCollectionType()) {
            return getForeignKeyForPropertyDomainClass(property, sessionFactoryBeanName);
        }

        return columnName;
    }

    protected String getForeignKeyForPropertyDomainClass(GrailsDomainClassProperty property,
            String sessionFactoryBeanName) {
        final String propertyName = property.getDomainClass().getPropertyName();
        NamingStrategy namingStrategy = getNamingStrategy(sessionFactoryBeanName);
        return namingStrategy.propertyToColumnName(propertyName) + FOREIGN_KEY_SUFFIX;
    }

    protected String getIndexColumnName(GrailsDomainClassProperty property, String sessionFactoryBeanName) {
        PropertyConfig pc = getPropertyConfig(property);
        if (pc != null && pc.getIndexColumn() != null && pc.getIndexColumn().getColumn() != null) {
            return pc.getIndexColumn().getColumn();
        }
        NamingStrategy namingStrategy = getNamingStrategy(sessionFactoryBeanName);
        return namingStrategy.propertyToColumnName(property.getName()) + UNDERSCORE + IndexedCollection.DEFAULT_INDEX_COLUMN_NAME;
    }

    protected String getIndexColumnType(GrailsDomainClassProperty property, String defaultType) {
        PropertyConfig pc = getPropertyConfig(property);
        if (pc != null && pc.getIndexColumn() != null && pc.getIndexColumn().getType() != null) {
            return getTypeName(property, pc.getIndexColumn(), getMapping(property.getDomainClass()));
        }
        return defaultType;
    }

    protected String getMapElementName(GrailsDomainClassProperty property, String sessionFactoryBeanName) {
        PropertyConfig pc = getPropertyConfig(property);

        if (hasJoinTableColumnNameMapping(pc)) {
            return pc.getJoinTable().getColumn().getName();
        }

        NamingStrategy namingStrategy = getNamingStrategy(sessionFactoryBeanName);
        return namingStrategy.propertyToColumnName(property.getName()) + UNDERSCORE + IndexedCollection.DEFAULT_ELEMENT_COLUMN_NAME;
    }

    protected boolean hasJoinTableColumnNameMapping(PropertyConfig pc) {
        return pc != null && pc.getJoinTable() != null && pc.getJoinTable().getColumn() != null && pc.getJoinTable().getColumn().getName() != null;
    }

    /**
     * Returns the constraints applied to the specified domain class property.
     *
     * @param grailsProp the property whose constraints will be returned
     * @return the <code>ConstrainedProperty</code> object representing the property's constraints
     */
    protected ConstrainedProperty getConstrainedProperty(GrailsDomainClassProperty grailsProp) {
        ConstrainedProperty constrainedProperty = null;
        Map<?, ?> constraints = grailsProp.getDomainClass().getConstrainedProperties();
        for (Iterator<?> constrainedPropertyIter = constraints.values().iterator(); constrainedPropertyIter.hasNext() && (constrainedProperty == null);) {
            ConstrainedProperty tmpConstrainedProperty = (ConstrainedProperty) constrainedPropertyIter.next();
            if (tmpConstrainedProperty.getPropertyName().equals(grailsProp.getName())) {
                constrainedProperty = tmpConstrainedProperty;
            }
        }
        return constrainedProperty;
    }

    /**
     * Interrogates the specified constraints looking for any constraints that would limit the
     * length of the property's value.  If such constraints exist, this method adjusts the length
     * of the column accordingly.
     *
     * @param column              the column that corresponds to the property
     * @param constrainedProperty the property's constraints
     */
    protected void bindStringColumnConstraints(Column column, ConstrainedProperty constrainedProperty) {
        Integer columnLength = constrainedProperty.getMaxSize();
        List<?> inListValues = constrainedProperty.getInList();
        if (columnLength != null) {
            column.setLength(columnLength.intValue());
        } else if (inListValues != null) {
            column.setLength(getMaxSize(inListValues));
        }
    }

    protected void bindNumericColumnConstraints(Column column, ConstrainedProperty constrainedProperty) {
        bindNumericColumnConstraints(column, constrainedProperty, null);
    }

    /**
     * Interrogates the specified constraints looking for any constraints that would limit the
     * precision and/or scale of the property's value.  If such constraints exist, this method adjusts
     * the precision and/or scale of the column accordingly.
     *
     * @param column              the column that corresponds to the property
     * @param constrainedProperty the property's constraints
     * @param cc the column configuration
     */
    protected void bindNumericColumnConstraints(Column column, ConstrainedProperty constrainedProperty, ColumnConfig cc) {
        int scale = Column.DEFAULT_SCALE;
        int precision = Column.DEFAULT_PRECISION;


        if(  cc != null && cc.getScale() > - 1) {
            column.setScale(cc.getScale());
        } else if (constrainedProperty.getScale() != null) {
            scale = constrainedProperty.getScale().intValue();
            column.setScale(scale);
        }


        if( cc != null && cc.getPrecision() > -1) {
            column.setPrecision(cc.getPrecision());
        }
        else {

            Comparable<?> minConstraintValue = constrainedProperty.getMin();
            Comparable<?> maxConstraintValue = constrainedProperty.getMax();

            int minConstraintValueLength = 0;
            if ((minConstraintValue != null) && (minConstraintValue instanceof Number)) {
                minConstraintValueLength = Math.max(
                        countDigits((Number) minConstraintValue),
                        countDigits(((Number) minConstraintValue).longValue()) + scale);
            }
            int maxConstraintValueLength = 0;
            if ((maxConstraintValue != null) && (maxConstraintValue instanceof Number)) {
                maxConstraintValueLength = Math.max(
                        countDigits((Number) maxConstraintValue),
                        countDigits(((Number) maxConstraintValue).longValue()) + scale);
            }

            if (minConstraintValueLength > 0 && maxConstraintValueLength > 0) {
                // If both of min and max constraints are setted we could use
                // maximum digits number in it as precision
                precision = NumberUtils.max(new int[]{minConstraintValueLength, maxConstraintValueLength});
            } else {
                // Overwise we should also use default precision
                precision = NumberUtils.max(new int[]{precision, minConstraintValueLength, maxConstraintValueLength});
            }

            column.setPrecision(precision);
        }
    }

    /**
     * @return a count of the digits in the specified number
     */
    protected int countDigits(Number number) {
        int numDigits = 0;

        if (number != null) {
            // Remove everything that's not a digit (e.g., decimal points or signs)
            String digitsOnly = number.toString().replaceAll("\\D", EMPTY_PATH);
            numDigits = digitsOnly.length();
        }

        return numDigits;
    }

    /**
     * @return the maximum length of the strings in the specified list
     */
    protected int getMaxSize(List<?> inListValues) {
        int maxSize = 0;

        for (Iterator<?> iter = inListValues.iterator(); iter.hasNext();) {
            String value = (String) iter.next();
            maxSize = Math.max(value.length(), maxSize);
        }

        return maxSize;
    }

    protected abstract String qualify(String prefix, String name);

    protected abstract String unqualify(String qualifiedName);

    protected abstract boolean isNotEmpty(String s);

    protected abstract Class<?> getGroovyAwareSingleTableEntityPersisterClass();

    protected abstract Class<?> getGroovyAwareJoinedSubclassEntityPersisterClass();

    protected abstract void handleLazyProxy(GrailsDomainClass domainClass, GrailsDomainClassProperty grailsProperty);

    protected abstract boolean identityEnumTypeSupports(Class<?> propertyType);
}

/**
 * A Collection type, for the moment only Set is supported
 *
 * @author Graeme
 */
abstract class CollectionType {

    protected Class<?> clazz;
    protected AbstractGrailsDomainBinder binder;

    protected static CollectionType SET;
    protected static CollectionType LIST;
    protected static CollectionType BAG;
    protected static CollectionType MAP;
    protected static boolean initialized;

    protected static final Map<Class<?>, CollectionType> INSTANCES = new HashMap<Class<?>, CollectionType>();

    public abstract Collection create(GrailsDomainClassProperty property, PersistentClass owner,
                                      String path, Mappings mappings, String sessionFactoryBeanName) throws MappingException;

    protected CollectionType(Class<?> clazz, AbstractGrailsDomainBinder binder) {
        this.clazz = clazz;
        this.binder = binder;
    }

    @Override
    public String toString() {
        return clazz.getName();
    }

    protected void createInstances() {

        if (initialized) {
            return;
        }

        initialized = true;

        SET = new CollectionType(Set.class, binder) {
            @Override
            public Collection create(GrailsDomainClassProperty property, PersistentClass owner,
                                     String path, Mappings mappings, String sessionFactoryBeanName) throws MappingException {
                org.hibernate.mapping.Set coll = new org.hibernate.mapping.Set(mappings, owner);
                coll.setCollectionTable(owner.getTable());
                binder.bindCollection(property, coll, owner, mappings, path, sessionFactoryBeanName);
                return coll;
            }
        };
        INSTANCES.put(Set.class, SET);
        INSTANCES.put(SortedSet.class, SET);

        LIST = new CollectionType(List.class, binder) {
            @Override
            public Collection create(GrailsDomainClassProperty property, PersistentClass owner,
                                     String path, Mappings mappings, String sessionFactoryBeanName) throws MappingException {
                org.hibernate.mapping.List coll = new org.hibernate.mapping.List(mappings, owner);
                coll.setCollectionTable(owner.getTable());
                binder.bindCollection(property, coll, owner, mappings, path, sessionFactoryBeanName);
                return coll;
            }
        };
        INSTANCES.put(List.class, LIST);

        BAG = new CollectionType(java.util.Collection.class, binder) {
            @Override
            public Collection create(GrailsDomainClassProperty property, PersistentClass owner,
                                     String path, Mappings mappings, String sessionFactoryBeanName) throws MappingException {
                Bag coll = new Bag(mappings, owner);
                coll.setCollectionTable(owner.getTable());
                binder.bindCollection(property, coll, owner, mappings, path, sessionFactoryBeanName);
                return coll;
            }
        };
        INSTANCES.put(java.util.Collection.class, BAG);

        MAP = new CollectionType(Map.class, binder) {
            @Override
            public Collection create(GrailsDomainClassProperty property, PersistentClass owner,
                                     String path, Mappings mappings, String sessionFactoryBeanName) throws MappingException {
                org.hibernate.mapping.Map map = new org.hibernate.mapping.Map(mappings, owner);
                binder.bindCollection(property, map, owner, mappings, path, sessionFactoryBeanName);
                return map;
            }
        };
        INSTANCES.put(Map.class, MAP);
    }

    public CollectionType collectionTypeForClass(Class<?> clazz) {
        createInstances();
        return INSTANCES.get(clazz);
    }
}
