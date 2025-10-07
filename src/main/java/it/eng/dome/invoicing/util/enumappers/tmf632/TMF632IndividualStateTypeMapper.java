package it.eng.dome.invoicing.util.enumappers.tmf632;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.module.SimpleModule;

import it.eng.dome.tmforum.tmf632.v4.model.IndividualStateType;

@Component
public class TMF632IndividualStateTypeMapper {

	@Bean("TMF632IndividualStateTypeMapperModule")
	public com.fasterxml.jackson.databind.Module getModule() {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(IndividualStateType.class, new IndividualStateTypeDeserializer());
		module.addSerializer(IndividualStateType.class, new IndividualStateTypeSerializer());
		return module;
	}

}
