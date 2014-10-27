/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import grails.validation.ValidationErrors;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;

import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodInvocation;
import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateTemplate;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.hibernate.SessionFactory;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

/**
 * @author Steven Devijver
 */
public abstract class AbstractDynamicPersistentMethod extends AbstractDynamicMethodInvocation {

    public static final String ERRORS_PROPERTY = "errors";

    private ClassLoader classLoader;
    private GrailsHibernateTemplate hibernateTemplate;
    private SessionFactory sessionFactory;
    GrailsApplication application;
    int defaultFlushMode;

    public AbstractDynamicPersistentMethod(Pattern pattern, SessionFactory sessionFactory, ClassLoader classLoader, GrailsApplication application, int defaultFlushMode) {
        super(pattern);
        Assert.notNull(sessionFactory, "Session factory is required!");
        this.classLoader = classLoader;
        Assert.notNull(application, "Constructor argument 'application' cannot be null");
        this.application = application;
        this.sessionFactory = sessionFactory;
        hibernateTemplate = new GrailsHibernateTemplate(sessionFactory, application, defaultFlushMode);
    }

    protected SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    protected GrailsHibernateTemplate getHibernateTemplate() {
        return hibernateTemplate;
    }

    @Override
    public Object invoke(Object target, String methodName, Object[] arguments) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            return doInvokeInternal(target, arguments);
        }
        finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    protected abstract Object doInvokeInternal(Object target, Object[] arguments);

    /**
     * This method will set the target object to read-only if it is contained with the Hibernate session,
     * Preventing Hibernate dirty-checking from persisting the instance
     *
     * @param target The target object
     */
    protected void setObjectToReadOnly(final Object target) {
        GrailsHibernateUtil.setObjectToReadyOnly(target, sessionFactory);
    }

    protected void setObjectToReadWrite(final Object target) {
        GrailsHibernateUtil.setObjectToReadWrite(target, sessionFactory);
    }

    /**
     * Initializes the Errors property on target.  The target will be assigned a new
     * Errors property.  If the target contains any binding errors, those binding
     * errors will be copied in to the new Errors property.
     *
     * @param target object to initialize
     * @return the new Errors object
     */
    protected Errors setupErrorsProperty(Object target) {
        MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(target.getClass());

        ValidationErrors errors = new ValidationErrors(target);

        Errors originalErrors = (Errors) mc.getProperty(target, ERRORS_PROPERTY);
        for (Object o : originalErrors.getFieldErrors()) {
            FieldError fe = (FieldError)o;
            if (fe.isBindingFailure()) {
                errors.addError(new FieldError(fe.getObjectName(),
                                               fe.getField(),
                                               fe.getRejectedValue(),
                                               fe.isBindingFailure(),
                                               fe.getCodes(),
                                               fe.getArguments(),
                                               fe.getDefaultMessage()));
            }
        }

        mc.setProperty(target, ERRORS_PROPERTY, errors);
        return errors;
    }
}
