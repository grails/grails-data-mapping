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

package org.grails.datastore.mapping.validation;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

/**
 * Exception thrown when a validation error occurs
 */
public class ValidationException extends DataIntegrityViolationException {

    private static final long serialVersionUID = 1;

    private String fullMessage;

    public ValidationException(String msg, Errors errors) {
        super(msg);
        fullMessage = formatErrors(errors, msg);
    }

    @Override
    public String getMessage() {
        return fullMessage;
    }

    public static String formatErrors(Errors errors, String msg ) {
        String ls = System.getProperty("line.separator");
        StringBuilder b = new StringBuilder();
        if (msg != null) {
            b.append(msg).append(" : ").append(ls);
        }

        for (ObjectError error : errors.getAllErrors()) {
            b.append(ls)
             .append(" - ")
             .append(error)
             .append(ls);
        }
        return b.toString();
    }
}
