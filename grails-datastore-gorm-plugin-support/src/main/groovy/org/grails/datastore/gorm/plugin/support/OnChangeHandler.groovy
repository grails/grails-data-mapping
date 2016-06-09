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
package org.grails.datastore.gorm.plugin.support

import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.reflect.AstUtils
import org.springframework.context.ApplicationContext
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.validation.Validator
/**
 * Common onChange handling logic.
 *
 * @author Graeme Rocher
 * @since 1.0
 * @deprecated No longer supported
 */
@Deprecated
abstract class OnChangeHandler extends DynamicMethodsConfigurer{

    OnChangeHandler(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    abstract String getDatastoreType()

    void onChange(plugin, Map event) {
        Class source = event.source
        if (!source || !event.ctx) {
            return
        }

        def application = event.application
        if (application.isArtefactOfType(AstUtils.DOMAIN_TYPE, source)) {
            final mappingContext = datastore.mappingContext
            final entity = mappingContext.addPersistentEntity(source, true)
            ApplicationContext ctx = event.ctx
            if (ctx.containsBean("${entity.name}Validator")) {
                mappingContext.addEntityValidator(entity, ctx.getBean("${entity.name}Validator", Validator))
            }
            configure()
        }

    }


}
