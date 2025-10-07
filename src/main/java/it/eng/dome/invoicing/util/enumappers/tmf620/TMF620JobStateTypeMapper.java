package it.eng.dome.invoicing.util.enumappers.tmf620;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.module.SimpleModule;

import it.eng.dome.tmforum.tmf620.v4.model.JobStateType;

@Component
public class TMF620JobStateTypeMapper {

	@Bean("TMF620JobStateTypeMapperModule")
	public com.fasterxml.jackson.databind.Module getModule() {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(JobStateType.class, new JobStateTypeDeserializer());
		module.addSerializer(JobStateType.class, new JobStateTypeSerializer());
		return module;
	}

}
