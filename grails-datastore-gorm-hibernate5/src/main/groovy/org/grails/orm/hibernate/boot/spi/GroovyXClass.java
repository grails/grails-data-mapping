package org.grails.orm.hibernate.boot.spi;

import org.hibernate.annotations.common.reflection.Filter;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMethod;
import org.hibernate.annotations.common.reflection.XProperty;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class GroovyXClass implements XClass
{
    private XClass xClass;
    private Class clazz;

    public Class toClassWithReflection(XClass xClass) throws ClassNotFoundException
    {

        Class javaClass = Class.forName(xClass.getName());
        return javaClass;
    }


    GroovyXClass(XClass xClass)
    {
        this.xClass = xClass;
        try
        {
            this.clazz = toClassWithReflection(xClass);
        } catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName()
    {
        return xClass.getName();
    }

    @Override
    public XClass getSuperclass()
    {
        return xClass.getSuperclass();
    }

    @Override
    public XClass[] getInterfaces()
    {
        return xClass.getInterfaces();
    }

    @Override
    public boolean isInterface()
    {
        return xClass.isInterface();
    }

    @Override
    public boolean isAbstract()
    {
        return xClass.isAbstract();
    }

    @Override
    public boolean isPrimitive()
    {
        return xClass.isPrimitive();
    }

    @Override
    public boolean isEnum()
    {
        return xClass.isEnum();
    }

    @Override
    public boolean isAssignableFrom(XClass c)
    {
        return xClass.isAssignableFrom(c);
    }

    @Override
    public List<XProperty> getDeclaredProperties(String accessType)
    {
        List<XProperty> results = new ArrayList();
        for(XProperty property : xClass.getDeclaredProperties(accessType)) {
            if (!ignoreXProperty(property))
                results.add(property);
        }

        return results;
    }

    private boolean ignoreXProperty(XProperty property)
    {
        String name = property.getName();
        if (name.contains("DirtyCheckable") || name.contains("GormValidateable"))
            return true;
        return false;
    }

    @Override
    public List<XProperty> getDeclaredProperties(String accessType, Filter filter)
    {
        return xClass.getDeclaredProperties(accessType, filter);
    }

    @Override
    public List<XMethod> getDeclaredMethods()
    {
        return xClass.getDeclaredMethods();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType)
    {
        return xClass.getAnnotation(annotationType);
    }

    @Override
    public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType)
    {
        return xClass.isAnnotationPresent(annotationType);
    }

    @Override
    public Annotation[] getAnnotations()
    {
        return xClass.getAnnotations();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroovyXClass that = (GroovyXClass) o;

        return xClass.equals(that.xClass);

    }

    Class toJavaClass() {
        return this.clazz;
    }

    @Override
    public int hashCode()
    {
        return xClass.hashCode();
    }
}
