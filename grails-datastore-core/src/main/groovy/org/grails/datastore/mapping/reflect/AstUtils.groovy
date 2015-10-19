/*
 * Copyright 2015 original authors
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
package org.grails.datastore.mapping.reflect

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.GenericsType
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression

import java.util.regex.Pattern


/**
 * Utility methods for dealing with Groovy ASTs
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@CompileStatic
class AstUtils {
    public static final String DOMAIN_TYPE = "Domain"
    public static final Parameter[] ZERO_PARAMETERS = new Parameter[0];
    public static final ArgumentListExpression ZERO_ARGUMENTS = new ArgumentListExpression();
    public static final ClassNode OBJECT_CLASS_NODE = new ClassNode(Object.class).getPlainNodeReference();

    private static final Set<String> TRANSFORMED_CLASSES = new HashSet<String>();

    /**
     * @return The names of the transformed entities for this context
     */
    public static Collection<String> getKnownEntityNames() {
        return Collections.unmodifiableCollection( TRANSFORMED_CLASSES );
    }

    /**
     * @param name Adds the name of a transformed entity
     */
    public static void addTransformedEntityName(String name) {
        TRANSFORMED_CLASSES.add(name)
    }
    /**
     * The name of the Grails application directory
     */

    public static final String GRAILS_APP_DIR = "grails-app";

    public static final String REGEX_FILE_SEPARATOR = "[\\\\/]"; // backslashes need escaping in regexes
    /*
     Domain path is always matched against the normalized File representation of an URL and
     can therefore work with slashes as separators.
     */
    public static Pattern DOMAIN_PATH_PATTERN = Pattern.compile(".+" + REGEX_FILE_SEPARATOR + GRAILS_APP_DIR + REGEX_FILE_SEPARATOR + "domain" + REGEX_FILE_SEPARATOR + "(.+)\\.(groovy|java)");

    private static final Map<String, ClassNode> emptyGenericsPlaceHoldersMap = Collections.emptyMap();

    /**
     * Checks whether the file referenced by the given url is a domain class
     *
     * @param url The URL instance
     * @return true if it is a domain class
     */
    public static boolean isDomainClass(URL url) {
        if (url == null) return false;

        return DOMAIN_PATH_PATTERN.matcher(url.getFile()).find();
    }

    public static ClassNode nonGeneric(ClassNode type) {
        return replaceGenericsPlaceholders(type, emptyGenericsPlaceHoldersMap);
    }

    @SuppressWarnings("unchecked")
    public static ClassNode nonGeneric(ClassNode type, final ClassNode wildcardReplacement) {
        return replaceGenericsPlaceholders(type, emptyGenericsPlaceHoldersMap, wildcardReplacement);
    }

    public static ClassNode replaceGenericsPlaceholders(ClassNode type, Map<String, ClassNode> genericsPlaceholders) {
        return replaceGenericsPlaceholders(type, genericsPlaceholders, null);
    }

    public static ClassNode replaceGenericsPlaceholders(ClassNode type, Map<String, ClassNode> genericsPlaceholders, ClassNode defaultPlaceholder) {
        if (type.isArray()) {
            return replaceGenericsPlaceholders(type.getComponentType(), genericsPlaceholders).makeArray();
        }

        if (!type.isUsingGenerics() && !type.isRedirectNode()) {
            return type.getPlainNodeReference();
        }

        if(type.isGenericsPlaceHolder() && genericsPlaceholders != null) {
            final ClassNode placeHolderType;
            if(genericsPlaceholders.containsKey(type.getUnresolvedName())) {
                placeHolderType = genericsPlaceholders.get(type.getUnresolvedName());
            } else {
                placeHolderType = defaultPlaceholder;
            }
            if(placeHolderType != null) {
                return placeHolderType.getPlainNodeReference();
            } else {
                return ClassHelper.make(Object.class).getPlainNodeReference();
            }
        }

        final ClassNode nonGen = type.getPlainNodeReference();

        if("java.lang.Object".equals(type.getName())) {
            nonGen.setGenericsPlaceHolder(false);
            nonGen.setGenericsTypes(null);
            nonGen.setUsingGenerics(false);
        } else {
            if(type.isUsingGenerics()) {
                GenericsType[] parameterized = type.getGenericsTypes();
                if (parameterized != null && parameterized.length > 0) {
                    GenericsType[] copiedGenericsTypes = new GenericsType[parameterized.length];
                    for (int i = 0; i < parameterized.length; i++) {
                        GenericsType parameterizedType = parameterized[i];
                        GenericsType copiedGenericsType = null;
                        if (parameterizedType.isPlaceholder() && genericsPlaceholders != null) {
                            ClassNode placeHolderType = genericsPlaceholders.get(parameterizedType.getName());
                            if(placeHolderType != null) {
                                copiedGenericsType = new GenericsType(placeHolderType.getPlainNodeReference());
                            } else {
                                copiedGenericsType = new GenericsType(ClassHelper.make(Object.class).getPlainNodeReference());
                            }
                        } else {
                            copiedGenericsType = new GenericsType(replaceGenericsPlaceholders(parameterizedType.getType(), genericsPlaceholders));
                        }
                        copiedGenericsTypes[i] = copiedGenericsType;
                    }
                    nonGen.setGenericsTypes(copiedGenericsTypes);
                }
            }
        }

        return nonGen;
    }

    public static void injectTrait(ClassNode classNode, Class traitClass) {
        ClassNode traitClassNode = ClassHelper.make(traitClass);
        boolean implementsTrait = false;
        boolean traitNotLoaded = false;
        try {
            implementsTrait = classNode.declaresInterface(traitClassNode);
        } catch (Throwable e) {
            // if we reach this point, the trait injector could not be loaded due to missing dependencies (for example missing servlet-api). This is ok, as we want to be able to compile against non-servlet environments.
            traitNotLoaded = true;
        }
        if (!implementsTrait && !traitNotLoaded) {
            final GenericsType[] genericsTypes = traitClassNode.getGenericsTypes();
            final Map<String, ClassNode> parameterNameToParameterValue = new LinkedHashMap<String, ClassNode>();
            if(genericsTypes != null) {
                for(GenericsType gt : genericsTypes) {
                    parameterNameToParameterValue.put(gt.getName(), classNode);
                }
            }
            classNode.addInterface(replaceGenericsPlaceholders(traitClassNode, parameterNameToParameterValue, classNode));
        }
    }
}
