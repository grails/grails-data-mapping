/*
 * Copyright 2014 original authors
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
package org.grails.datastore.gorm.support;

/**
 * Registers constraints for Grails 3.x
 *
 * @author Graeme Rocher
 */

import grails.validation.ConstrainedProperty;
import org.grails.datastore.gorm.validation.constraints.UniqueConstraintFactory;
import org.grails.datastore.mapping.core.Datastore;

public class ConstraintRegistrar {


    Datastore datastore;

    public ConstraintRegistrar(Datastore datastore) {
        this.datastore = datastore;

        ConstrainedProperty.registerNewConstraint("unique", new UniqueConstraintFactory(datastore));
    }
}
