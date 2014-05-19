package org.codehaus.groovy.grails.orm.hibernate.cfg

import static org.codehaus.groovy.grails.orm.hibernate.query.HibernateQueryConstants.ARGUMENT_FETCH_SIZE
import static org.codehaus.groovy.grails.orm.hibernate.query.HibernateQueryConstants.ARGUMENT_READ_ONLY
import static org.codehaus.groovy.grails.orm.hibernate.query.HibernateQueryConstants.ARGUMENT_TIMEOUT

import org.hibernate.Criteria
import org.hibernate.FlushMode
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.support.DefaultConversionService

class GrailsHibernateUtilTests extends GroovyTestCase {

	private ConversionService conversionService = new DefaultConversionService()

	void testPopulateArgumentsForCriteria_fetchSize() {
		assertMockedCriteriaCalledFor("setFetchSize", ARGUMENT_FETCH_SIZE, 10)
	}

	void testPopulateArgumentsForCriteria_timeout() {
		assertMockedCriteriaCalledFor("setTimeout", ARGUMENT_TIMEOUT, 60)
	}

	void testPopulateArgumentsForCriteria_readOnly() {
		assertMockedCriteriaCalledFor("setReadOnly", ARGUMENT_READ_ONLY, true)
	}

	// works for criteria methods with primitive arguments
	protected assertMockedCriteriaCalledFor(String methodName, String keyName, value) {
		Boolean methodCalled = false

		Criteria criteria = [
				(methodName): { passedValue ->
					assertEquals value, passedValue
					methodCalled = true
					return
				}
		] as Criteria

		GrailsHibernateUtil.populateArgumentsForCriteria(criteria, [(keyName): value], conversionService)
		assertTrue methodCalled
	}

	void testPopulateArgumentsForCriteria_flushMode() {
		Boolean methodCalled = false
		FlushMode value = FlushMode.MANUAL

		Criteria criteria = [
				setFlushMode: { FlushMode passedValue ->
					assertEquals value, passedValue
					methodCalled = true
					return
				}
		] as Criteria

		GrailsHibernateUtil.populateArgumentsForCriteria(criteria, [flushMode: value], conversionService)
		assertTrue methodCalled
	}
}
