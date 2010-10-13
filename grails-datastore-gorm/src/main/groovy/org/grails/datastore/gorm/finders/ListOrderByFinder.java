/* Copyright (C) 2010 SpringSource
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
package org.grails.datastore.gorm.finders;

import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.query.Query;
import org.springframework.datastore.mapping.reflect.NameUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The "listOrderBy*" static persistent method. Allows ordered listing of instances based on their properties.
 *
 * eg.
 * Account.listOrderByHolder();
 * Account.listOrderByHolder(max); // max results
 *
 * @author Graeme Rocher
 */
public class ListOrderByFinder implements FinderMethod{
    private static final Pattern METHOD_PATTERN = Pattern.compile("(listOrderBy)(\\w+)");
    private Pattern pattern = METHOD_PATTERN;
    private Datastore datastore;

    public ListOrderByFinder(Datastore datastore) {
        this.datastore = datastore;
    }

    public void setPattern(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public Object invoke(Class clazz, String methodName, Object[] arguments) {

        Matcher match = pattern.matcher(methodName);
        match.find();

        String nameInSignature = match.group(2);
        final String propertyName = NameUtils.decapitalize(nameInSignature);

        Query q = datastore.getCurrentSession().createQuery(clazz);

        if(arguments.length > 0 && (arguments[0] instanceof Map)) {
            DynamicFinder.populateArgumentsForCriteria(clazz, q, (Map) arguments[0]);
        }

        q.order(Query.Order.asc(propertyName));

        return invokeQuery(q);
    }

    protected Object invokeQuery(Query q) {
        return q.list();
    }

    public boolean isMethodMatch(String methodName) {
        return pattern.matcher(methodName.subSequence(0, methodName.length())).find();
    }
}
