/* Copyright (C) 2011 SpringSource
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
package org.grails.datastore.mapping.simpledb.engine;

import java.util.LinkedList;
import java.util.List;

/**
 * An implementation of the domain resolver which assumes there is no sharding -
 * i.e. always the same domain name for all the primary keys (for the same type
 * of {@link org.grails.datastore.mapping.model.PersistentEntity}
 */
public class ConstSimpleDBDomainResolver extends AbstractSimpleDBDomainResolver {

    private List<String> domains;

    public ConstSimpleDBDomainResolver(String entityFamily, String domainNamePrefix) {
        super(entityFamily, domainNamePrefix);
        domains = new LinkedList<String>();
        domains.add(this.entityFamily); // without sharding there is just one domain
    }

    public String resolveDomain(String id) {
        return entityFamily; // without sharding it is always the same one per PersistentEntity
    }

    public List<String> getAllDomainsForEntity() {
        return domains;
    }
}
