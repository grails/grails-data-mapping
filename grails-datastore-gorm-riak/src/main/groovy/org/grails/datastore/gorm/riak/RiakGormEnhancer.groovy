/*
 * Copyright (c) 2010 by J. Brisbin <jon@jbrisbin.com>
 *     Portions (c) 2010 by NPC International, Inc. or the
 *     original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.datastore.gorm.riak

import org.grails.datastore.gorm.AbstractDatastoreApi
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.SessionCallback
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.keyvalue.riak.core.QosParameters
import org.springframework.data.keyvalue.riak.core.RiakQosParameters
import org.springframework.data.keyvalue.riak.core.RiakTemplate
import org.springframework.data.keyvalue.riak.core.SimpleBucketKeyPair
import org.springframework.data.keyvalue.riak.mapreduce.*
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.riak.RiakDatastore

/**
 * @author J. Brisbin <jon@jbrisbin.com>
 */
class RiakGormEnhancer extends GormEnhancer {

    RiakGormEnhancer(datastore) {
        super(datastore)
    }

    RiakGormEnhancer(datastore, transactionManager) {
        super(datastore, transactionManager)
    }

    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        return new RiakGormStaticApi<D>(cls, datastore)
    }

    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        return new RiakGormInstanceApi<D>(cls, datastore)
    }
}

class RiakGormInstanceApi<D> extends GormInstanceApi<D> {

    RiakGormInstanceApi(Class<D> persistentClass, datastore) {
        super(persistentClass, datastore)
    }

    D save(D instance, Map params) {
        doInSession new SessionCallback() {
            def doInSession(Session session) {
                def currentQosParams = session.qosParameters
                if (params?.w || params?.dw) {
                    QosParameters qos = new RiakQosParameters()
                    qos.durableWriteThreshold = params?.dw
                    qos.writeThreshold = params?.w
                    session.qosParameters = qos
                }

                try {
                    return super.save(instance, params)
                }
                finally {
                    if (params?.w || params?.dw) {
                        session.qosParameters = currentQosParams
                    }
                }
            }
        }
    }
}

class RiakGormStaticApi<D> extends GormStaticApi<D> {

    MapReduceApi mapReduceApi

    RiakGormStaticApi(D persistentClass, datastore) {
        super(persistentClass, datastore)
        mapReduceApi = new MapReduceApi<D>(persistentClass, datastore)
    }

    MapReduceApi getMapreduce() {
        mapReduceApi
    }
}

class MapReduceApi<D> extends AbstractDatastoreApi {

    static Logger log = LoggerFactory.getLogger(MapReduceApi)

    Class<D> type

    MapReduceApi(Class<D> type, RiakDatastore datastore) {
        super(datastore)
        this.type = type
    }

    def invokeMethod(String methodName, args) {
        if (methodName != "execute") {
            return
        }

        log.debug "Invoking $methodName with ${args}"

        doInSession new SessionCallback() {
            def doInSession(Session session) {
                RiakTemplate riak = session.nativeInterface
                RiakMapReduceJob mr = new RiakMapReduceJob(riak)
                mr.addInputs([type.name])
                def params = args[0]
                if (params["map"]) {
                    def oper = extractMapReduceOperation(params["map"])
                    mr.addPhase(new RiakMapReducePhase(MapReducePhase.Phase.MAP, params["map"]["language"] ?: "javascript", oper))
                }
                if (params["reduce"]) {
                    def oper = extractMapReduceOperation(params["reduce"])
                    mr.addPhase(new RiakMapReducePhase(MapReducePhase.Phase.REDUCE, params["map"]["language"] ?: "javascript", oper))
                }
                riak.execute(mr)
            }
        }
    }

    protected MapReduceOperation extractMapReduceOperation(param) {
        def oper
        def lang = param.language ?: "javascript"
        if (lang == "javascript") {
            if (param.source) {
                oper = new JavascriptMapReduceOperation(param.source)
            } else if (param.bucket && param.key) {
                def bkp = new SimpleBucketKeyPair(param.bucket, param.key)
                oper = new JavascriptMapReduceOperation(bkp)
            }
        } else if (lang == "erlang") {
            oper = new ErlangMapReduceOperation(param.module, param.function)
        }
        oper
    }
}
