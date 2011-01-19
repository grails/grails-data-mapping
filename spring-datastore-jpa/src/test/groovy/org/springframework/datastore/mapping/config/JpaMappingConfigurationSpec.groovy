package org.springframework.datastore.mapping.config

import javax.persistence.OneToMany 
import javax.persistence.OneToOne 
import org.springframework.datastore.mapping.jpa.config.JpaMappingConfigurationStrategy;
import org.springframework.datastore.mapping.jpa.config.JpaMappingContext;
import org.springframework.datastore.mapping.jpa.config.JpaMappingFactory;

import spock.lang.Specification 

class JpaMappingConfigurationSpec extends Specification{
	
	
	void "Test that a JPA entity is detected as such"() {
		
		when:
			def configStrategy = new JpaMappingConfigurationStrategy()
		
		then:
			configStrategy.isPersistentEntity(JpaEntity.class) == true
			configStrategy.isPersistentEntity(JpaMappingConfigurationSpec) == false
	}
	
	void "Test persistent properties are valid"() {
		when:
			def configStrategy = new JpaMappingConfigurationStrategy(new JpaMappingFactory())
			def properties = configStrategy.getPersistentProperties(JpaEntity, new JpaMappingContext()).sort { it.name }
	
		then:
			properties.size() == 3
			
			properties[0] instanceof org.springframework.datastore.mapping.model.types.OneToMany
			properties[0].name == "many"
			
			properties[1].name == "name"
			properties[2].name == "other"
			properties[2] instanceof org.springframework.datastore.mapping.model.types.OneToOne
	}

}
@org.springframework.datastore.mapping.jpa.config.JpaEntity
class JpaEntity {
	Long id
	
	String name
	
	@OneToOne
	JpaOther other
	
	@OneToMany
	Set<JpaOther> many
	
}
@org.springframework.datastore.mapping.jpa.config.JpaEntity
class JpaOther {
	Long id
	
	String name
	
}