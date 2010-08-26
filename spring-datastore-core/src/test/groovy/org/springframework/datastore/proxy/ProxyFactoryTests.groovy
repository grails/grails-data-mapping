package org.springframework.datastore.proxy

import org.springframework.aop.framework.ProxyFactory
import org.springframework.aop.TargetSource
import org.springframework.aop.target.EmptyTargetSource
import org.junit.Test

class ProxyFactoryTests {

  @Test
  void testSpringProxyFactory() {
    TargetSource ts = [
            getTargetClass: {-> MyProxied },
            isStatic:{-> false },
            releaseTarget:{},
            getTarget: {
              println "Created instance!"
              new MyProxied()
            }
    ] as TargetSource

    ProxyFactory proxyFactory = new ProxyFactory();
	proxyFactory.setTargetSource(ts);
	proxyFactory.setProxyTargetClass(true);


    println proxyFactory.getProxy()
  }
}
class MyProxied {
  String name
}
