/*
 * Copyright 2017 the original author or authors.
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
package grails.gorm.multitenancy;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.*;

/**
 * <p>An AST transformation that makes a particular class or method applicable to the tenant id returned by the passed closure. For example:</p>
 *
 * <pre>
 * class FooService {
 *  {@code @CurrentTenant}
 *   void updateFoo() {
 *       ...
 *   }
 * }
 * </pre>
 *
 * @since 6.1
 * @author Graeme Rocher
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@GroovyASTTransformationClass("org.grails.datastore.gorm.multitenancy.transform.TenantTransform")
public @interface CurrentTenant {
}
