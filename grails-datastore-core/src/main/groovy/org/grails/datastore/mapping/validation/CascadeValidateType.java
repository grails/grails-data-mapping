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
package org.grails.datastore.mapping.validation;

/**
 * Defines a set of cascade validation options that are propagated to the associated entity.
 */
public enum CascadeValidateType {
    /** Fall back to the CascadeType and ownership of the association to decide whether to cascade validation **/
    DEFAULT,

    /** Don't cascade validations at all, only entities that are actually saved via cascade will be validated **/
    NONE,

    /** Only cascade validation for associations which are owned by the parent entity **/
    OWNED,

    /** Only cascade validation for entities which are dirty. If the object isn't DirtyCheckable, this will fall back to DEFAULT **/
    DIRTY;

    public static CascadeValidateType fromMappedName(String name) {
        return CascadeValidateType.valueOf(name.toUpperCase());
    }
}
