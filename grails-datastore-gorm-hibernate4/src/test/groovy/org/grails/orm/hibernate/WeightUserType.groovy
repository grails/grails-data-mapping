package org.grails.orm.hibernate

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

import org.hibernate.engine.spi.SessionImplementor
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

	Object nullSafeGet(ResultSet resultSet, String[] names, SessionImplementor session, owner) {
		Weight result
		int pounds = resultSet.getInt(names[0])
		if (!resultSet.wasNull()) {
			result = new Weight(pounds)
		}
		return result
	 }

	void nullSafeSet(PreparedStatement statement, value, int index, SessionImplementor session){
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
