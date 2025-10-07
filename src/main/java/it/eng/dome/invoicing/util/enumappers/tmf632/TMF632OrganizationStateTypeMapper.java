package it.eng.dome.invoicing.util.enumappers.tmf632;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.module.SimpleModule;

import it.eng.dome.tmforum.tmf632.v4.model.OrganizationStateType;

@Component
public class TMF632OrganizationStateTypeMapper {

	@Bean("TMF632OrganizationStateTypeMapperModule")
	public com.fasterxml.jackson.databind.Module getModule() {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(OrganizationStateType.class, new OrganizationStateTypeDeserializer());
		module.addSerializer(OrganizationStateType.class, new OrganizationStateTypeSerializer());
		return module;
	}

}
