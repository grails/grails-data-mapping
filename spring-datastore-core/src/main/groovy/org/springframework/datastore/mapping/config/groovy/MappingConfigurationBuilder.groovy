package org.springframework.datastore.mapping.config.groovy

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class MappingConfigurationBuilder {

  Object target
  Map properties = [:]
  Class propertyClass

  MappingConfigurationBuilder(target, Class propertyClass) {
    this.target = target;
    this.propertyClass = propertyClass
  }

  def invokeMethod(String name, args) {
    if(args.size()>0) {

      if(target.hasProperty(name)) {
        target[name] = args.size() == 1 ? args[0] : args
      }
      else {
        if(args[0] instanceof Map) {

          properties[name] = args[0].asType(propertyClass)
        }
      }
    }
  }

  public void evaluate ( Closure callable ) {
      if(callable) {
        callable.delegate = this
        callable.resolveStrategy = Closure.DELEGATE_ONLY
        callable.call()
      }
  }
}
