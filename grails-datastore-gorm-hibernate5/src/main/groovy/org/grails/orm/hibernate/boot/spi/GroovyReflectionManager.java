package org.grails.orm.hibernate.boot.spi;


import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;


public class GroovyReflectionManager extends JavaReflectionManager
{
    @Override
    public XClass toXClass(Class clazz)
    {
        XClass xClass = super.toXClass(clazz);
        return new GroovyXClass(xClass);
    }

    @Override
    public Class toClass(XClass xClass)
    {
        if (xClass instanceof GroovyXClass)
            return ((GroovyXClass)xClass).toJavaClass();
        return super.toClass(xClass);
    }



    public boolean equals(XClass class1, Class class2)
    {
        if (class1 == null)
        {
            return class2 == null;
        }

        if (class1 instanceof GroovyXClass)
            return ((GroovyXClass)class1).toJavaClass().equals(class2);

        return super.equals(class1,class2);
    }

}
