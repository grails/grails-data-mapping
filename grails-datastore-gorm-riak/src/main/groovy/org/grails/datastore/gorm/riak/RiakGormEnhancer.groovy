/*
 * Copyright (c) 2010 by NPC International, Inc.
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

package org.grails.datastore.gorm.riak

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
class RiakGormEnhancer extends GormEnhancer {

  RiakGormEnhancer(datastore) {
    super(datastore);
  }

  RiakGormEnhancer(datastore, transactionManager) {
    super(datastore, transactionManager);
  }

  protected GormStaticApi getStaticApi(Class cls) {
    return new RiakGormStaticApi(cls, datastore)
  }

  protected GormInstanceApi getInstanceApi(Class cls) {
    return new RiakGormInstanceApi(cls, datastore)
  }

}

class RiakGormInstanceApi extends GormInstanceApi {

  RiakGormInstanceApi(persistentClass, datastore) {
    super(persistentClass, datastore);
  }
}

class RiakGormStaticApi extends GormStaticApi {

  RiakGormStaticApi(persistentClass, datastore) {
    super(persistentClass, datastore);
  }
}