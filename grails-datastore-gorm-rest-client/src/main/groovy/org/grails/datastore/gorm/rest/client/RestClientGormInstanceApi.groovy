/* Copyright (C) 2013 original authors
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

package org.grails.datastore.gorm.rest.client

import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.mapping.core.Datastore
import org.codehaus.groovy.grails.plugins.converters.api.ConvertersApi
import org.grails.datastore.mapping.rest.client.RestClientSession
import groovy.transform.CompileStatic
import grails.plugins.rest.client.RequestCustomizer
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionCallback
import groovy.transform.TypeCheckingMode

/**
 * Extensions to the instance API for REST
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class RestClientGormInstanceApi<D> extends GormInstanceApi<D> {

    RestClientGormInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
    }

    /**
     * Converter an instance from one format to another
     *
     * @param instance The instance
     * @param clazz The type to convert to
     * @return the converted object
     */
    public Object asType(Object instance, Class<?> clazz) {
        new ConvertersApi().asType(instance, clazz)
    }




    /**
     * Save the instance to the given URL
     *
     * @param instance The instance
     * @param url The URL
     * @return The instance if saved, null otherwise
     */
    public Object save(Object instance, @DelegatesTo(RequestCustomizer) Closure requestCustomizer) {
        put(instance, requestCustomizer)
    }

    /**
     * Deletes an instances for the given URL
     *
     * @param instance The instance
     * @param url The URL to send the DELETE request to
     */
    @CompileStatic(TypeCheckingMode.SKIP) // TODO: Report JIRA, removal causes GroovyCastException
    public void delete(Object instance,@DelegatesTo(RequestCustomizer) Closure requestCustomizer) {
        execute({ Session session ->
            withRequestCustomizer(session, instance, requestCustomizer) {
                delete(instance)
            }
        } as SessionCallback)
    }

    /**
     * Synonym for save()
     *
     * @param instance The instance
     * @return The instance if it was saved, null otherwise
     */
    @CompileStatic(TypeCheckingMode.SKIP) // TODO: Report JIRA, removal causes GroovyCastException
    public Object put(Object instance, @DelegatesTo(RequestCustomizer)  Closure requestCustomizer ) {
        execute({ Session session ->
            withRequestCustomizer(session, instance, requestCustomizer) {
                save(instance)
            }
        } as SessionCallback)
    }

    /**
     * Synonym for insert()
     *
     * @param instance The instance
     * @return The instance if it was saved, null otherwise
     */
    @CompileStatic(TypeCheckingMode.SKIP) // TODO: Report JIRA, removal causes GroovyCastException
    public Object post(Object instance, @DelegatesTo(RequestCustomizer)  Closure requestCustomizer) {
        execute({ Session session ->
            withRequestCustomizer(session, instance, requestCustomizer) {
                insert(instance)
            }
        } as SessionCallback)
    }


    /**
     * Save the instance to the given URL
     *
     * @param instance The instance
     * @param url The URL
     * @return The instance if saved, null otherwise
     */
    public Object save(Object instance, URL url,@DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        put(instance, url, requestCustomizer)
    }

    /**
     * Deletes an instances for the given URL
     *
     * @param instance The instance
     * @param url The URL to send the DELETE request to
     */
    @CompileStatic(TypeCheckingMode.SKIP) // TODO: Report JIRA, removal causes GroovyCastException
    public void delete(Object instance, URL url,@DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        execute({ Session session ->
            withCustomUrl(session,instance, url) {
                withRequestCustomizer(session,instance, requestCustomizer) {
                    delete(instance)
                }
            }
        } as SessionCallback)
    }

    /**
     * Synonym for save()
     *
     * @param instance The instance
     * @return The instance if it was saved, null otherwise
     */
    @CompileStatic(TypeCheckingMode.SKIP) // TODO: Report JIRA, removal causes GroovyCastException
    public Object put(Object instance, URL url = null, @DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        execute({ Session session ->
            withCustomUrl(session, instance, url) {
                withRequestCustomizer(session, instance, requestCustomizer) {
                    save(instance)
                }
            }
        } as SessionCallback)
    }

    /**
     * Synonym for insert()
     *
     * @param instance The instance
     * @return The instance if it was saved, null otherwise
     */
    @CompileStatic(TypeCheckingMode.SKIP) // TODO: Report JIRA, removal causes GroovyCastException
    public Object post(Object instance, URL url= null, @DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        execute({ Session session ->
            withCustomUrl(session, instance, url) {
                withRequestCustomizer(session, instance, requestCustomizer) {
                    insert(instance)
                }
            }
        } as SessionCallback)
    }


    /**
     * Save the instance to the given URL
     *
     * @param instance The instance
     * @param url The URL
     * @return The instance if saved, null otherwise
     */
    public Object save(Object instance, Map params, URL url, @DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        put(instance, params,url, requestCustomizer)
    }

    /**
     * Deletes an instances for the given URL
     *
     * @param instance The instance
     * @param url The URL to send the DELETE request to
     */
    @CompileStatic(TypeCheckingMode.SKIP) // TODO: Report JIRA, removal causes GroovyCastException
    public void delete(Object instance, Map params, URL url,@DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        execute({ Session session ->
            withCustomUrl(session,instance, url) {
                withRequestCustomizer(session,instance, requestCustomizer) {
                    delete(instance)
                }
            }
        } as SessionCallback)
    }

    /**
     * Synonym for save()
     *
     * @param instance The instance
     * @return The instance if it was saved, null otherwise
     */
    @CompileStatic(TypeCheckingMode.SKIP) // TODO: Report JIRA, removal causes GroovyCastException
    public Object put(Object instance, Map params, URL url,@DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        execute({ Session session ->
            withCustomUrl(session, instance, url) {
                withRequestCustomizer(session, instance, requestCustomizer) {
                    save(instance, params)
                }
            }
        } as SessionCallback)
    }

    /**
     * Synonym for insert()
     *
     * @param instance The instance
     * @return The instance if it was saved, null otherwise
     */
    @CompileStatic(TypeCheckingMode.SKIP) // TODO: Report JIRA, removal causes GroovyCastException
    public Object post(Object instance, Map params, URL url, @DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        execute({ Session session ->
            withCustomUrl(session, instance, url) {
                withRequestCustomizer(session, instance, requestCustomizer) {
                    insert(instance, params)
                }
            }
        } as SessionCallback)
    }

    protected Object withCustomUrl(Session currentSession, Object instance, URL url, Closure callable) {
        if (url) {
            currentSession.setAttribute(instance, RestClientSession.ATTRIBUTE_URL, url)
        }
        try {
            return callable.call()
        } finally {
            if (url) {
                currentSession.setAttribute(instance, RestClientSession.ATTRIBUTE_URL, null)
            }
        }
    }

    protected Object withRequestCustomizer(Session currentSession, Object instance, @DelegatesTo(RequestCustomizer) Closure customizer, Closure callable) {
        if (customizer) {
            currentSession.setAttribute(instance, RestClientSession.ATTRIBUTE_REQUEST_CUSTOMIZER, customizer)
        }
        try {
            callable.call()
        } finally {
            if (customizer) {
                currentSession.setAttribute(instance, RestClientSession.ATTRIBUTE_URL, null)
            }
        }
    }
}
