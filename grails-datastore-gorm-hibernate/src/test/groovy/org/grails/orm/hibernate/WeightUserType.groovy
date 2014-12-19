package org.grails.orm.hibernate

import org.hibernate.HibernateException

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

import org.hibernate.usertype.UserType

class WeightUserType implements UserType {

	private static final int[] SQL_TYPES = [Types.INTEGER]
	int[] sqlTypes() { SQL_TYPES }

	Class returnedClass() { Weight }

	boolean equals(x, y) {
		if (x == y) {
			return true
		}
		if (x == null || y == null) {
			return false
		}
		x.equals(y)
	}

	int hashCode(x) { x.hashCode() }

    @Override
    Object nullSafeGet(ResultSet resultSet, String[] strings, Object o) throws HibernateException, SQLException {
        Weight result
        int pounds = resultSet.getInt(strings[0])
        if (!resultSet.wasNull()) {
            result = new Weight(pounds)
        }
        return result
    }

    @Override
    void nullSafeSet(PreparedStatement statement, Object value, int index) throws HibernateException, SQLException {
        if (value == null) {
            statement.setNull(index)
        }
        else {
            Integer pounds = value.pounds
            statement.setInt(index, pounds)
        }
    }


	Object deepCopy(value) { value }

	boolean isMutable() { false }

	Serializable disassemble(value) { value }

	Object assemble(Serializable state, owner) { state }

	Object replace(original, target, owner) { original }
}
