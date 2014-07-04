package org.grails.datastore.gorm.cassandra;

import org.grails.datastore.mapping.model.PersistentEntity;


public class InList extends org.grails.datastore.gorm.finders.MethodExpression.InList{

    public InList(Class<?> targetClass, String propertyName) {
        super(targetClass, propertyName);       
    }

    @Override
    public void convertArguments(PersistentEntity persistentEntity) {
        
    }

}
