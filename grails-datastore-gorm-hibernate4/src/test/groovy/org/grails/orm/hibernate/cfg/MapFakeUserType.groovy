package org.grails.orm.hibernate.cfg

import org.hibernate.engine.spi.SessionImplementor

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

import org.hibernate.HibernateException
import org.hibernate.usertype.UserType

public class MapFakeUserType implements UserType {

    int[] sqlTypes() { Types.VARCHAR }
    Class returnedClass() { MapFakeUserType }

    public boolean equals(Object x, Object y) throws HibernateException {
        return x == y;
    }

    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor sessionImplementor, Object o) throws HibernateException, SQLException {
        String name = rs.getString(names[0])
        rs.wasNull() ? null : Collections.singletonMap("foo",name)
    }

    @Override
    void nullSafeSet(PreparedStatement ps, Object value, int index, SessionImplementor sessionImplementor) throws HibernateException, SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR)
        }
        else {
            ps.setString(index, value.foo)
        }
    }

    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    public boolean isMutable() {
        return false;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable)value;
    }

    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}