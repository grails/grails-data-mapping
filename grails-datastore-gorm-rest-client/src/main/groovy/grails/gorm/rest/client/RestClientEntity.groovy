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
package grails.gorm.rest.client

import grails.plugins.rest.client.RequestCustomizer
import grails.plugins.rest.client.async.AsyncRestBuilder
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionCallback
import org.grails.datastore.mapping.rest.client.RestClientDatastore
import org.grails.datastore.mapping.rest.client.RestClientSession
import org.grails.plugins.converters.api.ConvertersApi


/**
 * Extends the default {@link org.grails.datastore.gorm.GormEntity} method with REST client specific features
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@CompileStatic
trait RestClientEntity<D> extends GormEntity<D> {

    /**
     * Converter an instance from one format to another
     *
     * @param instance The instance
     * @param clazz The type to convert to
     * @return the converted object
     */
    public <T> T asType(Class<T> clazz) {
        (T)new ConvertersApi().asType(this, clazz)
    }

    /**
     * Save the instance to the given URL
     *
     * @param instance The instance
     * @param url The URL
     * @return The instance if saved, null otherwise
     */
    public D save(@DelegatesTo(RequestCustomizer) Closure requestCustomizer) {
        put(requestCustomizer)
    }

    /**
     * Deletes an instances for the given URL
     *
     * @param instance The instance
     * @param url The URL to send the DELETE request to
     */
    public void delete(@DelegatesTo(RequestCustomizer) Closure requestCustomizer) {
        RestClientEntity<D> me = this
        withSession ({ Session session ->
            withRequestCustomizer(session, me, requestCustomizer) {
                me.delete()
            }
        } )
    }

    /**
     * Synonym for save()
     *
     * @param instance The instance
     * @return The instance if it was saved, null otherwise
     */
    public D put(@DelegatesTo(RequestCustomizer)  Closure requestCustomizer ) {
        RestClientEntity<D> me = this
        withSession ({ Session session ->
            withRequestCustomizer(session, me, requestCustomizer) {
                (D)me.save()
            }
        } )
    }

    /**
     * Synonym for insert()
     *
     * @param instance The instance
     * @return The instance if it was saved, null otherwise
     */
    @CompileStatic(TypeCheckingMode.SKIP) // TODO: Report JIRA, removal causes GroovyCastException
    public D post(@DelegatesTo(RequestCustomizer)  Closure requestCustomizer) {
        RestClientEntity<D> me = this
        withSession ({ Session session ->
            withRequestCustomizer(session, me, requestCustomizer) {
                (D)me.insert()
            }
        } )
    }


    /**
     * Save the instance to the given URL
     *
     * @param instance The instance
     * @param url The URL
     * @return The instance if saved, null otherwise
     */
    public D save(URL url,@DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        put(url, requestCustomizer)
    }

    /**
     * Deletes an instances for the given URL
     *
     * @param instance The instance
     * @param url The URL to send the DELETE request to
     */
    public void delete(URL url,@DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        RestClientEntity<D> me = this
        withSession ({ Session session ->
            withCustomUrl(session,me, url) {
                withRequestCustomizer(session,me, requestCustomizer) {
                    me.delete()
                }
            }
        })
    }

    /**
     * Synonym for save()
     *
     * @param instance The instance
     * @return The instance if it was saved, null otherwise
     */
    public D put(URL url = null, @DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        RestClientEntity<D> me = this
        withSession ({ Session session ->
            withCustomUrl(session, me, url) {
                withRequestCustomizer(session, me, requestCustomizer) {
                    (D)me.save()
                }
            }
        })
    }

    /**
     * Synonym for insert()
     *
     * @param instance The instance
     * @return The instance if it was saved, null otherwise
     */
    public D post(URL url= null, @DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        RestClientEntity<D> me = this
        withSession ({ Session session ->
            withCustomUrl(session, me, url) {
                withRequestCustomizer(session, me, requestCustomizer) {
                    (D)me.insert()
                }
            }
        } )
    }


    /**
     * Save the instance to the given URL
     *
     * @param instance The instance
     * @param url The URL
     * @return The instance if saved, null otherwise
     */
    public D save(Map params, URL url, @DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        put(params,url, requestCustomizer)
    }

    /**
     * Deletes an instances for the given URL
     *
     * @param instance The instance
     * @param url The URL to send the DELETE request to
     */
    public void delete(Map params, URL url,@DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        RestClientEntity<D> me = this
        withSession ({ Session session ->
            withCustomUrl(session,me, url) {
                withRequestCustomizer(session,me, requestCustomizer) {
                    me.delete()
                }
            }
        } )
    }

    /**
     * Synonym for save()
     *
     * @param instance The instance
     * @return The instance if it was saved, null otherwise
     */
    public D put(Map params, URL url,@DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        RestClientEntity<D> me = this
        withSession ({ Session session ->
            withCustomUrl(session, me, url) {
                withRequestCustomizer(session, me, requestCustomizer) {
                    (D)me.save(params)
                }
            }
        })
    }

    /**
     * Synonym for insert()
     *
     * @param instance The instance
     * @return The instance if it was saved, null otherwise
     */
    public D post(Map params, URL url, @DelegatesTo(RequestCustomizer) Closure requestCustomizer = null) {
        RestClientEntity<D> me = this
        withSession ({ Session session ->
            withCustomUrl(session, me, url) {
                withRequestCustomizer(session, me, requestCustomizer) {
                    (D)me.insert(params)
                }
            }
        } )
    }

    private <T> T withCustomUrl(Session currentSession, Object instance, URL url, Closure<T> callable) {
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

    private <T> T withRequestCustomizer(Session currentSession, Object instance, @DelegatesTo(RequestCustomizer) Closure customizer, Closure<T> callable) {
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
    /**
     * Obtains the RestBuilder instance used by this domain
     */
    static AsyncRestBuilder getRestBuilder() {
        def staticApi = GormEnhancer.findStaticApi(this)
        def datastore = staticApi.getDatastore()
        ((RestClientDatastore)datastore).asyncRestClients.get(staticApi.gormPersistentEntity)
    }

    /**
     * Execute a closure whose first argument is a reference to the current session.
     *
     * @param callable the closure
     * @return The result of the closure
     */
    static <T> T withSession(Closure<T> callable) {
        GormEnhancer.findStaticApi(this).withSession callable
    }

    static List<D> getAll(URL url, Serializable... ids) {
        withSession ({ Session session ->
            withCustomUrl(session, url) {
                (List<D>)GormEnhancer.findStaticApi(this).getAll(ids)
            }
        })
    }

    /**
     * Retrieve all the objects for the given identifiers
     * @param ids The identifiers to operate against
     * @return A list of identifiers
     */
    static List<D> getAll(Serializable... ids) {
        GormEnhancer.findStaticApi(this).getAll ids
    }

    /**
     * Retrieves and object from the datastore. eg. Book.get(1)
     */
    static D get(Serializable id) {
        GormEnhancer.findStaticApi(this).get(id)
    }

    static D get(URL url, Serializable id) {
        withSession ({ Session session ->
            withCustomUrl(session, url) {
                (D)GormEnhancer.findStaticApi(this).get(id)
            }
        } )

    }

    static List<D> getAll(URL url, Iterable<Serializable> ids, @DelegatesTo(RequestCustomizer) Closure customizer) {
        withSession ({ Session session ->
            withRequestCustomizer(session, customizer) {
                withCustomUrl(session, url) {
                    (List<D>)GormEnhancer.findStaticApi(this).getAll((Serializable[])ids.toList().toArray())
                }
            }
        })
    }

    static D get(URL url, Serializable id, @DelegatesTo(RequestCustomizer) Closure customizer) {
        withSession ({ Session session ->
            withRequestCustomizer(session, customizer) {
                withCustomUrl(session, url) {
                    (D)GormEnhancer.findStaticApi(this).get(id)
                }
            }
        } )

    }


    private static <T> T withCustomUrl(Session currentSession, URL url, Closure<T> callable) {
        def persistentEntity = currentSession.mappingContext.getPersistentEntity(this.name)
        if (url) {
            currentSession.setAttribute(persistentEntity, RestClientSession.ATTRIBUTE_URL, url)
        }
        try {
            return callable.call()
        } finally {
            if (url) {
                currentSession.setAttribute(persistentEntity, RestClientSession.ATTRIBUTE_URL, null)
            }
        }
    }

    private static <T> T withRequestCustomizer(Session currentSession, @DelegatesTo(RequestCustomizer) Closure customizer, Closure<T> callable) {
        def persistentEntity = currentSession.mappingContext.getPersistentEntity(this.name)
        if (customizer) {
            currentSession.setAttribute(persistentEntity, RestClientSession.ATTRIBUTE_REQUEST_CUSTOMIZER, customizer)
        }
        try {
            callable.call()
        } finally {
            if (customizer) {
                currentSession.setAttribute(persistentEntity, RestClientSession.ATTRIBUTE_URL, null)
            }
        }
    }

}